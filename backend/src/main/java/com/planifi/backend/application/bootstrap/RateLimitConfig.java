package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record RateLimitConfig(
        @NotNull Boolean enabled,
        @Min(1) int requestsPerMinute,
        @Min(0) int burst
) {
}
