package com.planifi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.ErrorResponse;
import com.planifi.backend.application.ApiKeyService;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public ApiKeyAuthenticationFilter(SecurityProperties securityProperties,
                                      ApiKeyService apiKeyService,
                                      ObjectMapper objectMapper,
                                      Tracer tracer) {
        this.securityProperties = securityProperties;
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!securityProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (shouldBypass(request) || hasAuthentication() || hasBearerToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = securityProperties.getApiKeyHeader();
        String apiKey = request.getHeader(headerName);

        AuthenticatedApiKey principal = resolvePrincipal(apiKey);
        if (principal == null) {
            writeUnauthorized(response, "AUTH_API_KEY_INVALID", "API key inválida o ausente");
            return;
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                apiKey,
                AuthorityUtils.NO_AUTHORITIES
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/api-keys")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/v1/openapi");
    }

    private boolean hasAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.startsWith("Bearer ");
    }

    private boolean isStaticKey(String apiKey) {
        if (securityProperties.getStaticKeys() == null) {
            return false;
        }
        return securityProperties.getStaticKeys().stream()
                .anyMatch(candidate -> candidate.equals(apiKey));
    }

    private AuthenticatedApiKey resolvePrincipal(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        if (isStaticKey(apiKey)) {
            return new AuthenticatedApiKey(null, null);
        }
        return apiKeyService.findActiveKey(apiKey)
                .map(activeKey -> new AuthenticatedApiKey(activeKey.getId(), activeKey.getUserId()))
                .orElse(null);
    }

    private void writeUnauthorized(HttpServletResponse response, String errorCode, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(errorCode, message, traceId());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String traceId() {
        if (tracer.currentSpan() == null) {
            return "unknown";
        }
        return tracer.currentSpan().context().traceId();
    }
}
