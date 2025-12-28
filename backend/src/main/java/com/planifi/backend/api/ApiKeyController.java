package com.planifi.backend.api;

import com.planifi.backend.api.dto.ApiKeySecretResponse;
import com.planifi.backend.api.dto.CreateApiKeyRequest;
import com.planifi.backend.application.ApiKeySecret;
import com.planifi.backend.application.ApiKeyService;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.config.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/api-keys")
@Validated
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeySecretResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateApiKeyRequest request) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        ApiKeySecret secret = apiKeyService.createKey(authenticatedUser.userId(), request.name());
        return toResponse(secret);
    }

    @PostMapping("/{apiKeyId}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeySecretResponse rotate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @PathVariable UUID apiKeyId) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        ApiKeySecret secret = apiKeyService.rotateKey(authenticatedUser.userId(), apiKeyId);
        return toResponse(secret);
    }

    @PostMapping("/{apiKeyId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @PathVariable UUID apiKeyId) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        apiKeyService.revokeKey(authenticatedUser.userId(), apiKeyId);
    }

    private ApiKeySecretResponse toResponse(ApiKeySecret secret) {
        return new ApiKeySecretResponse(
                secret.id(),
                secret.name(),
                secret.apiKey(),
                secret.createdAt()
        );
    }

    private AuthenticatedUser requireUser(AuthenticatedUser user) {
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
