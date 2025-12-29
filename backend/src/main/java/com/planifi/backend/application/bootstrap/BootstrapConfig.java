package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record BootstrapConfig(
        @NotBlank String version,
        @NotNull @Valid BootstrapStorageConfig storage,
        @NotNull @Valid BootstrapSecurityConfig security,
        @JsonSetter(nulls = Nulls.AS_EMPTY) @Valid List<BootstrapUserConfig> users
) {
    public BootstrapConfig {
        if (users == null) {
            users = List.of();
        } else {
            users = List.copyOf(new ArrayList<>(users));
        }
    }
}
