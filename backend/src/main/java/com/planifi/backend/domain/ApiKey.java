package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, name = "key_hash", unique = true)
    private String keyHash;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected ApiKey() {
        // JPA only
    }

    public ApiKey(UUID id, UUID userId, String name, String keyHash, OffsetDateTime createdAt,
                  OffsetDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.keyHash = keyHash;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void revoke(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
