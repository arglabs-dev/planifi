package com.planifi.backend.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiKeySecret(UUID id, UUID userId, String name, String apiKey,
                           OffsetDateTime createdAt) {
}
