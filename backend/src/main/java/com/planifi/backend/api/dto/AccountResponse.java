package com.planifi.backend.api.dto;

import com.planifi.backend.domain.AccountType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        OffsetDateTime createdAt
) {
}
