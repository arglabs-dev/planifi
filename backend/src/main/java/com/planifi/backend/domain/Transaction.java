package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, name = "account_id")
    private UUID accountId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, name = "occurred_on")
    private LocalDate occurredOn;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    protected Transaction() {
        // JPA only
    }

    public Transaction(UUID id, UUID accountId, BigDecimal amount, LocalDate occurredOn,
                       String description, OffsetDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
