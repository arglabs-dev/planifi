package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, name = "request_hash", length = 255)
    private String requestHash;

    @Column(name = "response_body")
    private String responseBody;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    protected IdempotencyKey() {
        // JPA only
    }

    public IdempotencyKey(UUID id,
                          String idempotencyKey,
                          String requestHash,
                          String responseBody,
                          String status,
                          OffsetDateTime createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
