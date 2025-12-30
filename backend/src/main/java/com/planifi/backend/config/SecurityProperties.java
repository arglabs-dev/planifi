package com.planifi.backend.config;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "planifi.security")
public class SecurityProperties {

    private boolean enabled = true;
    private String apiKeyHeader = "X-MCP-API-Key";
    private String apiKeyPrefix = "pln";
    private List<String> staticKeys = List.of();
    @Valid
    private CorsProperties cors = new CorsProperties();
    @Valid
    private HeadersProperties headers = new HeadersProperties();
    @Valid
    private RateLimitProperties rateLimit = new RateLimitProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public String getApiKeyPrefix() {
        return apiKeyPrefix;
    }

    public void setApiKeyPrefix(String apiKeyPrefix) {
        this.apiKeyPrefix = apiKeyPrefix;
    }

    public List<String> getStaticKeys() {
        return staticKeys;
    }

    public void setStaticKeys(List<String> staticKeys) {
        this.staticKeys = staticKeys;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public HeadersProperties getHeaders() {
        return headers;
    }

    public void setHeaders(HeadersProperties headers) {
        this.headers = headers;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class CorsProperties {
        private List<String> allowedOrigins = List.of();
        private List<String> allowedMethods =
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders =
                List.of("Authorization", "Content-Type", "Idempotency-Key",
                        "correlation-id", "request-id", "X-MCP-API-Key");
        private List<String> exposedHeaders =
                List.of("correlation-id", "request-id", "traceId");
        private boolean allowCredentials = false;
        private long maxAge = 3600L;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class HeadersProperties {
        private boolean enabled = true;
        private boolean hstsEnabled = true;
        private long hstsMaxAgeSeconds = 31536000;
        private boolean hstsIncludeSubDomains = true;
        private boolean hstsPreload = true;
        private boolean frameOptionsDeny = true;
        private String contentSecurityPolicy =
                "default-src 'none'; frame-ancestors 'none'; sandbox";
        private ReferrerPolicyHeaderWriter.ReferrerPolicy referrerPolicy =
                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public long getHstsMaxAgeSeconds() {
            return hstsMaxAgeSeconds;
        }

        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }

        public boolean isHstsIncludeSubDomains() {
            return hstsIncludeSubDomains;
        }

        public void setHstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
            this.hstsIncludeSubDomains = hstsIncludeSubDomains;
        }

        public boolean isHstsPreload() {
            return hstsPreload;
        }

        public void setHstsPreload(boolean hstsPreload) {
            this.hstsPreload = hstsPreload;
        }

        public boolean isFrameOptionsDeny() {
            return frameOptionsDeny;
        }

        public void setFrameOptionsDeny(boolean frameOptionsDeny) {
            this.frameOptionsDeny = frameOptionsDeny;
        }

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public ReferrerPolicyHeaderWriter.ReferrerPolicy getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(
                ReferrerPolicyHeaderWriter.ReferrerPolicy referrerPolicy
        ) {
            this.referrerPolicy = referrerPolicy;
        }
    }
}
