package com.planifi.backend.observability;

import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.config.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "correlation-id";
    public static final String REQUEST_ID_HEADER = "request-id";
    public static final String MDC_CORRELATION_ID = "correlation-id";
    public static final String MDC_REQUEST_ID = "request-id";
    public static final String MDC_USER_ID = "user-id";

    private static final Logger logger = LoggerFactory.getLogger(RequestContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String correlationId = resolveOrGenerate(request, CORRELATION_ID_HEADER);
        String requestId = resolveOrGenerate(request, REQUEST_ID_HEADER);

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, requestId);
        enrichUser(SecurityContextHolder.getContext().getAuthentication());

        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            enrichUser(SecurityContextHolder.getContext().getAuthentication());
            long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logger.info("request.completed",
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("status", response.getStatus()),
                    StructuredArguments.keyValue("latency_ms", latencyMs));
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String resolveOrGenerate(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return UUID.randomUUID().toString();
    }

    private void enrichUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            MDC.put(MDC_USER_ID, hashValue(user.userId().toString()));
        } else if (principal instanceof AuthenticatedApiKey apiKey && apiKey.userId() != null) {
            MDC.put(MDC_USER_ID, hashValue(apiKey.userId().toString()));
        }
    }

    private String hashValue(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
