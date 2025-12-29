package com.planifi.backend.api.dto;

import com.planifi.backend.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull AccountType type
) {
}
