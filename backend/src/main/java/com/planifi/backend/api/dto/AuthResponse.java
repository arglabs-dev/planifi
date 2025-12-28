package com.planifi.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String token,
        String tokenType,
        OffsetDateTime expiresAt
) {
}
