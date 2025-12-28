package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.ApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);
}
