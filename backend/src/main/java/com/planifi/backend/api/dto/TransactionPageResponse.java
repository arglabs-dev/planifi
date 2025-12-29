package com.planifi.backend.api.dto;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
