package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.config.SecurityProperties;
import com.planifi.backend.domain.ApiKey;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.infrastructure.persistence.ApiKeyRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private ApiKeyHasher apiKeyHasher;
    private SecurityProperties securityProperties;
    private ApiKeyService apiKeyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        apiKeyHasher = new ApiKeyHasher();
        securityProperties = new SecurityProperties();
        securityProperties.setApiKeyPrefix("pln");
        objectMapper = new ObjectMapper().findAndRegisterModules();
        apiKeyService = new ApiKeyService(apiKeyRepository, idempotencyKeyRepository, apiKeyHasher,
                securityProperties, objectMapper);
    }

    @Test
    void createKeyHashesAndPersists() {
        UUID userId = UUID.randomUUID();
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeySecret secret = apiKeyService.createKey(userId, "MCP principal", "idem-1");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getName()).isEqualTo("MCP principal");
        assertThat(saved.getKeyHash()).isEqualTo(apiKeyHasher.hash(secret.apiKey()));
        assertThat(secret.apiKey()).startsWith("pln_" + saved.getId() + "_");
    }

    @Test
    void rotateKeyRevokesPreviousAndCreatesNew() {
        UUID userId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        ApiKey existing = new ApiKey(apiKeyId, userId, "MCP", "hash",
                OffsetDateTime.now().minusDays(1), null);
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(apiKeyRepository.findByIdAndUserId(apiKeyId, userId))
                .thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeySecret rotated = apiKeyService.rotateKey(userId, apiKeyId, "idem-2");

        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(rotated.id()).isNotEqualTo(apiKeyId);
        verify(apiKeyRepository, times(2)).save(any());
    }

    @Test
    void revokeKeyFailsWhenMissing() {
        UUID userId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(apiKeyRepository.findByIdAndUserId(apiKeyId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeKey(userId, apiKeyId, "idem-3"))
                .isInstanceOf(ApiKeyNotFoundException.class);
    }

    @Test
    void createKeyReplaysStoredResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        ApiKeySecret stored = new ApiKeySecret(UUID.randomUUID(), userId, "MCP",
                "pln_key_secret", OffsetDateTime.now());
        String responseBody = objectMapper.writeValueAsString(stored);
        IdempotencyKey record = new IdempotencyKey(
                UUID.randomUUID(),
                "idem-4",
                apiKeyService.hashRequest("create", userId.toString(), "MCP"),
                responseBody,
                "COMPLETED",
                OffsetDateTime.now());

        when(idempotencyKeyRepository.findByIdempotencyKey("idem-4"))
                .thenReturn(Optional.of(record));

        ApiKeySecret response = apiKeyService.createKey(userId, "MCP", "idem-4");

        assertThat(response).usingRecursiveComparison().isEqualTo(stored);
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayload() {
        UUID userId = UUID.randomUUID();
        IdempotencyKey record = new IdempotencyKey(
                UUID.randomUUID(),
                "idem-5",
                apiKeyService.hashRequest("create", userId.toString(), "MCP"),
                null,
                "COMPLETED",
                OffsetDateTime.now());
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-5"))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> apiKeyService.createKey(userId, "Other", "idem-5"))
                .isInstanceOf(IdempotencyKeyReuseException.class);
    }
}
