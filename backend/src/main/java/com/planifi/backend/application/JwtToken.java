package com.planifi.backend.application;

import java.time.OffsetDateTime;

public record JwtToken(String token, OffsetDateTime expiresAt) {
}
