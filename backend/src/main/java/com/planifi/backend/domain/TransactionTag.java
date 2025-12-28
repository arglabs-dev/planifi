package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction_tags")
public class TransactionTag {

    @EmbeddedId
    private TransactionTagId id;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    protected TransactionTag() {
        // JPA only
    }

    public TransactionTag(TransactionTagId id, OffsetDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public TransactionTagId getId() {
        return id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
