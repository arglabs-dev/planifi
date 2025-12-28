package com.planifi.backend.application;

import java.util.UUID;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(UUID apiKeyId) {
        super("API key " + apiKeyId + " not found");
    }
}
