package com.planifi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_ERROR = "RATE_LIMIT_EXCEEDED";

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupEpochMillis = new AtomicLong(0);

    public RateLimitingFilter(SecurityProperties securityProperties,
                              ObjectMapper objectMapper,
                              Tracer tracer) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitProperties rateLimit = securityProperties.getRateLimit();
        if (!securityProperties.isEnabled()
                || rateLimit == null
                || !Boolean.TRUE.equals(rateLimit.isEnabled())
                || !isSensitive(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveClientKey(request, rateLimit);
        cleanupExpiredBuckets(rateLimit);
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || isExpired(existing, rateLimit, now)) {
                return new BucketState(newBucket(rateLimit), now);
            }
            existing.touch(now);
            return existing;
        }).bucket();
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Limit",
                    String.valueOf(rateLimit.getRequestsPerMinute() + rateLimit.getBurst()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
                RATE_LIMIT_ERROR,
                "Se excedió el límite de solicitudes. Intenta más tarde.",
                traceId()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private boolean isSensitive(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (securityProperties.getRateLimit() == null
                || securityProperties.getRateLimit().getSensitivePaths() == null) {
            return false;
        }
        return securityProperties.getRateLimit().getSensitivePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Bucket newBucket(RateLimitProperties rateLimit) {
        int capacity = rateLimit.getRequestsPerMinute() + rateLimit.getBurst();
        Refill refill = Refill.greedy(rateLimit.getRequestsPerMinute(), Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request, RateLimitProperties rateLimit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedApiKey) {
                Object credentials = authentication.getCredentials();
                if (credentials instanceof String apiKey && StringUtils.hasText(apiKey)) {
                    return "api-key:" + sha256(apiKey);
                }
            } else if (principal instanceof AuthenticatedUser user) {
                if (user.userId() != null) {
                    return "user:" + user.userId();
                }
            }
        }

        String apiKeyHeader = securityProperties.getApiKeyHeader();
        String apiKey = request.getHeader(apiKeyHeader);
        if (StringUtils.hasText(apiKey) && isTrustedApiKey(apiKey)) {
            return "api-key:" + sha256(apiKey);
        }
        if (rateLimit.isTrustForwardedFor()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                String ip = forwardedFor.split(",")[0].trim();
                if (StringUtils.hasText(ip)) {
                    return "ip:" + ip;
                }
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isTrustedApiKey(String apiKey) {
        if (securityProperties.getStaticKeys() == null) {
            return false;
        }
        return securityProperties.getStaticKeys().stream()
                .anyMatch(candidate -> candidate.equals(apiKey));
    }

    private void cleanupExpiredBuckets(RateLimitProperties rateLimit) {
        long now = System.currentTimeMillis();
        long cleanupIntervalMillis = Duration.ofSeconds(rateLimit.getCleanupIntervalSeconds()).toMillis();
        long lastCleanup = lastCleanupEpochMillis.get();
        if (now - lastCleanup < cleanupIntervalMillis) {
            return;
        }
        if (!lastCleanupEpochMillis.compareAndSet(lastCleanup, now)) {
            return;
        }
        buckets.entrySet().removeIf(entry -> isExpired(entry.getValue(), rateLimit, now));
    }

    private boolean isExpired(BucketState state, RateLimitProperties rateLimit, long now) {
        long ttlMillis = Duration.ofSeconds(rateLimit.getBucketTtlSeconds()).toMillis();
        return now - state.lastAccessEpochMillis() > ttlMillis;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return "hash-error";
        }
    }

    private String traceId() {
        if (tracer.currentSpan() == null) {
            return "unknown";
        }
        return tracer.currentSpan().context().traceId();
    }

    private static final class BucketState {
        private final Bucket bucket;
        private volatile long lastAccessEpochMillis;

        private BucketState(Bucket bucket, long lastAccessEpochMillis) {
            this.bucket = bucket;
            this.lastAccessEpochMillis = lastAccessEpochMillis;
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccessEpochMillis() {
            return lastAccessEpochMillis;
        }

        private void touch(long now) {
            this.lastAccessEpochMillis = now;
        }
    }
}
