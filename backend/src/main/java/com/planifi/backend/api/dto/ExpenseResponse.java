package com.planifi.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        LocalDate occurredOn,
        String description,
        OffsetDateTime createdAt
) {
}
