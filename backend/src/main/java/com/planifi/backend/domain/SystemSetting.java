package com.planifi.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(nullable = false, name = "setting_key", length = 100)
    private String key;

    @Column(nullable = false, name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    protected SystemSetting() {
        // JPA only
    }

    public SystemSetting(String key, String value, OffsetDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
