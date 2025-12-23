package com.planifi.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @NotBlank String description
) {
}
