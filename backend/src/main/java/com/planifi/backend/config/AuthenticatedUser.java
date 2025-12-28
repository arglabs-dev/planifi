package com.planifi.backend.config;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email) {
}
