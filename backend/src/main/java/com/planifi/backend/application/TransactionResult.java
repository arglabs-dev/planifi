package com.planifi.backend.application;

import com.planifi.backend.domain.Tag;
import com.planifi.backend.domain.Transaction;
import java.util.List;

public record TransactionResult(
        Transaction transaction,
        List<Tag> tags
) {
}
