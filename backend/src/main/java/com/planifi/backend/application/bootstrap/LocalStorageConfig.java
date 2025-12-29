package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record LocalStorageConfig(
        @NotBlank String basePath
) {
}
