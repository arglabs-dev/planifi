package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planifi.backend.config.SecurityProperties;
import com.planifi.backend.domain.ApiKey;
import com.planifi.backend.infrastructure.persistence.ApiKeyRepository;
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

    private ApiKeyHasher apiKeyHasher;
    private SecurityProperties securityProperties;
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyHasher = new ApiKeyHasher();
        securityProperties = new SecurityProperties();
        securityProperties.setApiKeyPrefix("pln");
        apiKeyService = new ApiKeyService(apiKeyRepository, apiKeyHasher, securityProperties);
    }

    @Test
    void createKeyHashesAndPersists() {
        UUID userId = UUID.randomUUID();
        when(apiKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeySecret secret = apiKeyService.createKey(userId, "MCP principal");

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
        when(apiKeyRepository.findByIdAndUserId(apiKeyId, userId))
                .thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeySecret rotated = apiKeyService.rotateKey(userId, apiKeyId);

        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(rotated.id()).isNotEqualTo(apiKeyId);
        verify(apiKeyRepository, times(2)).save(any());
    }

    @Test
    void revokeKeyFailsWhenMissing() {
        UUID userId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        when(apiKeyRepository.findByIdAndUserId(apiKeyId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeKey(userId, apiKeyId))
                .isInstanceOf(ApiKeyNotFoundException.class);
    }
}
