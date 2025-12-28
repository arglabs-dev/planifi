package com.planifi.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeySecretResponse(
        UUID id,
        String name,
        String apiKey,
        OffsetDateTime createdAt
) {
}
