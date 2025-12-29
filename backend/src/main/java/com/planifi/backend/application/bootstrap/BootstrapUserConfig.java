package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record BootstrapUserConfig(
        @NotBlank @Email String email,
        String fullName,
        String password,
        String passwordHash,
        @JsonSetter(nulls = Nulls.AS_EMPTY) @Valid List<BootstrapAccountConfig> accounts
) {
    public BootstrapUserConfig {
        if (accounts == null) {
            accounts = List.of();
        } else {
            accounts = List.copyOf(new ArrayList<>(accounts));
        }
    }
}
