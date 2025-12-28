package com.planifi.backend.application;

import com.planifi.backend.config.SecurityProperties;
import com.planifi.backend.domain.ApiKey;
import com.planifi.backend.infrastructure.persistence.ApiKeyRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final int SECRET_BYTES = 32;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ApiKeyHasher apiKeyHasher,
                         SecurityProperties securityProperties) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.securityProperties = securityProperties;
    }

    public ApiKeySecret createKey(UUID userId, String name) {
        return createKeyForUser(userId, name);
    }

    @Transactional
    public ApiKeySecret rotateKey(UUID userId, UUID apiKeyId) {
        ApiKey existing = apiKeyRepository.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (existing.getRevokedAt() == null) {
            existing.revoke(OffsetDateTime.now());
            apiKeyRepository.save(existing);
        }

        return createKeyForUser(userId, existing.getName());
    }

    @Transactional
    public void revokeKey(UUID userId, UUID apiKeyId) {
        ApiKey existing = apiKeyRepository.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (existing.getRevokedAt() == null) {
            existing.revoke(OffsetDateTime.now());
            apiKeyRepository.save(existing);
        }
    }

    public Optional<ApiKey> findActiveKey(String rawKey) {
        String keyHash = apiKeyHasher.hash(rawKey);
        return apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash);
    }

    private ApiKeySecret createKeyForUser(UUID userId, String name) {
        UUID keyId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        String secret = generateSecret();
        String apiKeyValue = String.format("%s_%s_%s", securityProperties.getApiKeyPrefix(),
                keyId, secret);
        String hashed = apiKeyHasher.hash(apiKeyValue);

        ApiKey apiKey = new ApiKey(keyId, userId, name, hashed, createdAt, null);
        apiKeyRepository.save(apiKey);

        return new ApiKeySecret(keyId, userId, name, apiKeyValue, createdAt);
    }

    private String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTES];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
