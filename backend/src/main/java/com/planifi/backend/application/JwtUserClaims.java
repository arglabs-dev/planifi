package com.planifi.backend.application;

import java.util.UUID;

public record JwtUserClaims(UUID userId, String email) {
}
