package com.planifi.backend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RateLimitProperties {

    @NotNull
    private Boolean enabled = true;

    @Min(1)
    private int requestsPerMinute = 60;

    @Min(0)
    private int burst = 20;

    private List<String> sensitivePaths = List.of(
            "/api/v1/auth/**",
            "/api/v1/api-keys/**"
    );

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getBurst() {
        return burst;
    }

    public void setBurst(int burst) {
        this.burst = burst;
    }

    public List<String> getSensitivePaths() {
        return sensitivePaths;
    }

    public void setSensitivePaths(List<String> sensitivePaths) {
        this.sensitivePaths = sensitivePaths;
    }
}
