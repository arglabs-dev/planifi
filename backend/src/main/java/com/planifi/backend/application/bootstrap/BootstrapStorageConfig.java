package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record BootstrapStorageConfig(
        @NotBlank String provider,
        @Valid LocalStorageConfig local
) {
}
