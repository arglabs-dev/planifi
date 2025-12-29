package com.planifi.backend.api;

import com.planifi.backend.api.dto.CreateTransactionRequest;
import com.planifi.backend.api.dto.TransactionPageResponse;
import com.planifi.backend.api.dto.TagResponse;
import com.planifi.backend.api.dto.TransactionResponse;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.application.TransactionPageResult;
import com.planifi.backend.application.TransactionResult;
import com.planifi.backend.application.TransactionService;
import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.domain.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public TransactionPageResponse listTransactions(
            Authentication authentication,
            @RequestParam("accountId") @NotNull UUID accountId,
            @RequestParam("from") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "50") @Min(1) @Max(200) int size) {
        UUID userId = requireUserId(authentication);
        TransactionPageResult result = transactionService
                .listTransactions(userId, accountId, from, to, page, size);
        return toPageResponse(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {
        UUID userId = requireUserId(authentication);
        boolean createMissingTags = Boolean.TRUE.equals(request.createMissingTags());
        TransactionResult result = transactionService.createTransaction(
                userId,
                request.accountId(),
                request.amount(),
                request.occurredOn(),
                request.description(),
                request.tags(),
                createMissingTags,
                idempotencyKey
        );
        return toResponse(result.transaction(), result.tags());
    }

    private TransactionPageResponse toPageResponse(TransactionPageResult result) {
        List<TransactionResponse> items = result.items().stream()
                .map(entry -> toResponse(entry.transaction(), entry.tags()))
                .toList();
        return new TransactionPageResponse(
                items,
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages()
        );
    }

    private TransactionResponse toResponse(Transaction transaction, List<Tag> tags) {
        List<TagResponse> tagResponses = tags.stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt()))
                .toList();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getOccurredOn(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                tagResponses
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
