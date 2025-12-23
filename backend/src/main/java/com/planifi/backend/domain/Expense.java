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
@Table(name = "expenses")
public class Expense {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    protected Expense() {
        // JPA only
    }

    public Expense(UUID id, BigDecimal amount, LocalDate occurredOn, String description,
                   OffsetDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
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
