package com.planifi.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TagResponse(
        UUID id,
        String name,
        OffsetDateTime createdAt
) {
}
