package com.planifi.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planifi.security")
public class SecurityProperties {

    private boolean enabled = true;
    private String apiKeyHeader = "X-API-Key";
    private List<String> staticKeys = List.of();

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

    public List<String> getStaticKeys() {
        return staticKeys;
    }

    public void setStaticKeys(List<String> staticKeys) {
        this.staticKeys = staticKeys;
    }
}
