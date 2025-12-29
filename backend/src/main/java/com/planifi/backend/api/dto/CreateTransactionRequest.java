package com.planifi.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @NotBlank String description,
        List<@NotBlank String> tags,
        @NotNull Boolean createMissingTags
) {
}
