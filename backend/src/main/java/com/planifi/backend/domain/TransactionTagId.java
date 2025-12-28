package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TransactionTagId implements Serializable {

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

    protected TransactionTagId() {
        // JPA only
    }

    public TransactionTagId(UUID transactionId, UUID tagId) {
        this.transactionId = transactionId;
        this.tagId = tagId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionTagId that = (TransactionTagId) o;
        return Objects.equals(transactionId, that.transactionId)
            && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, tagId);
    }
}
