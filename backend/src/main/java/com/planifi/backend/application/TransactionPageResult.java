package com.planifi.backend.application;

import java.util.List;

public record TransactionPageResult(
        List<TransactionResult> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
