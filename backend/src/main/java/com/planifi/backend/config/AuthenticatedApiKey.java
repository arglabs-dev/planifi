package com.planifi.backend.config;

import java.util.UUID;

public record AuthenticatedApiKey(UUID apiKeyId, UUID userId) {
}
