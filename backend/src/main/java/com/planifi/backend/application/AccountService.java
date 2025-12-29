package com.planifi.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final String DEFAULT_CURRENCY = "MXN";

    private final AccountRepository accountRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public AccountService(AccountRepository accountRepository,
                          IdempotencyKeyRepository idempotencyKeyRepository,
                          ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Account createAccount(UUID userId, String name, AccountType type, String idempotencyKey) {
        String requestHash = hashRequest("create-account", userId.toString(), name, type.name());
        return executeIdempotent(idempotencyKey, requestHash, Account.class,
                () -> createAccountInternal(userId, name, type));
    }

    @Transactional(readOnly = true)
    public List<Account> listActiveAccounts(UUID userId) {
        return accountRepository.findByUserIdAndDisabledAtIsNullOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void disableAccount(UUID userId, UUID accountId, String idempotencyKey) {
        String requestHash = hashRequest("disable-account", userId.toString(), accountId.toString());
        executeIdempotent(idempotencyKey, requestHash, Void.class, () -> {
            Account account = accountRepository.findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            if (account.getDisabledAt() == null) {
                account.disable(OffsetDateTime.now());
                accountRepository.save(account);
            }
            return null;
        });
    }

    private Account createAccountInternal(UUID userId, String name, AccountType type) {
        Account account = new Account(
                UUID.randomUUID(),
                userId,
                name,
                type,
                DEFAULT_CURRENCY,
                OffsetDateTime.now(),
                null
        );
        return accountRepository.save(account);
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
