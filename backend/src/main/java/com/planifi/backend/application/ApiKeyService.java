package com.planifi.backend.application;

import com.planifi.backend.config.SecurityProperties;
import com.planifi.backend.domain.ApiKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.infrastructure.persistence.ApiKeyRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final int SECRET_BYTES = 32;

    private final ApiKeyRepository apiKeyRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         IdempotencyKeyRepository idempotencyKeyRepository,
                         ApiKeyHasher apiKeyHasher,
                         SecurityProperties securityProperties,
                         ObjectMapper objectMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApiKeySecret createKey(UUID userId, String name, String idempotencyKey) {
        String requestHash = hashRequest("create", userId.toString(), name);
        return executeIdempotent(idempotencyKey, requestHash, ApiKeySecret.class,
                () -> createKeyForUser(userId, name));
    }

    @Transactional
    public ApiKeySecret rotateKey(UUID userId, UUID apiKeyId, String idempotencyKey) {
        String requestHash = hashRequest("rotate", userId.toString(), apiKeyId.toString());
        return executeIdempotent(idempotencyKey, requestHash, ApiKeySecret.class,
                () -> rotateKeyInternal(userId, apiKeyId));
    }

    @Transactional
    public void revokeKey(UUID userId, UUID apiKeyId, String idempotencyKey) {
        String requestHash = hashRequest("revoke", userId.toString(), apiKeyId.toString());
        executeIdempotent(idempotencyKey, requestHash, Void.class, () -> {
            revokeKeyInternal(userId, apiKeyId);
            return null;
        });
    }

    @Transactional
    public void revokeKeyInternal(UUID userId, UUID apiKeyId) {
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

    private ApiKeySecret rotateKeyInternal(UUID userId, UUID apiKeyId) {
        ApiKey existing = apiKeyRepository.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (existing.getRevokedAt() == null) {
            existing.revoke(OffsetDateTime.now());
            apiKeyRepository.save(existing);
        }

        return createKeyForUser(userId, existing.getName());
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

    private <T> T executeIdempotent(String idempotencyKey,
                                    String requestHash,
                                    Class<T> responseType,
                                    Supplier<T> action) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey stored = existing.get();
            if (!stored.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException(idempotencyKey);
            }
            if (responseType == Void.class || stored.getResponseBody() == null) {
                return null;
            }
            try {
                return objectMapper.readValue(stored.getResponseBody(), responseType);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to read idempotent response", ex);
            }
        }

        T response = action.get();
        String responseBody = null;
        if (responseType != Void.class) {
            try {
                responseBody = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to persist idempotent response", ex);
            }
        }
        IdempotencyKey record = new IdempotencyKey(
                UUID.randomUUID(),
                idempotencyKey,
                requestHash,
                responseBody,
                "COMPLETED",
                OffsetDateTime.now());
        idempotencyKeyRepository.save(record);
        return response;
    }

    String hashRequest(String operation, String... values) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
        digest.update(operation.getBytes(StandardCharsets.UTF_8));
        for (String value : values) {
            digest.update((byte) ':');
            digest.update(value.getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
