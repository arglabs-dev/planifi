package com.planifi.backend.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @NotBlank @Size(max = 255) String description,
        @Valid @Size(max = 25) List<@NotBlank @Size(max = 80) String> tags,
        Boolean createMissingTags
) {
}
