package com.planifi.backend.api;

import com.planifi.backend.api.dto.CreateTransactionRequest;
import com.planifi.backend.api.dto.TransactionResponse;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.application.TransactionService;
import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {
        UUID userId = requireUserId(authentication);
        Transaction transaction = transactionService.createTransaction(userId, request);
        List<String> tags = request.tags() == null ? List.of() : request.tags();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getOccurredOn(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                tags
        );
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId();
        }
        if (principal instanceof AuthenticatedApiKey authenticatedApiKey) {
            if (authenticatedApiKey.userId() == null) {
                throw new InvalidCredentialsException();
            }
            return authenticatedApiKey.userId();
        }
        throw new InvalidCredentialsException();
    }
}
