package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.planifi.backend.api.dto.CreateApiKeyRequest;
import com.planifi.backend.application.ApiKeyService;
import com.planifi.backend.application.InvalidCredentialsException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiKeyControllerTest {

    @Test
    void createRequiresAuthenticatedUser() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyController controller = new ApiKeyController(apiKeyService);

        assertThatThrownBy(() -> controller.create(
                null,
                "idem-12345678",
                new CreateApiKeyRequest("MCP principal")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rotateRequiresAuthenticatedUser() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyController controller = new ApiKeyController(apiKeyService);

        assertThatThrownBy(() -> controller.rotate(
                null,
                "idem-12345678",
                UUID.randomUUID()))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
