package com.planifi.backend.api.dto;

public record ErrorResponse(
        String errorCode,
        String message,
        String traceId
) {
}
