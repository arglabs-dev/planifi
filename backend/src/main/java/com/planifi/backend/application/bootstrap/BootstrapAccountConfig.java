package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record BootstrapAccountConfig(
        @NotBlank String name,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
