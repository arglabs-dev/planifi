package com.planifi.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        LocalDate occurredOn,
        String description,
        OffsetDateTime createdAt,
        List<TagResponse> tags
) {
}
