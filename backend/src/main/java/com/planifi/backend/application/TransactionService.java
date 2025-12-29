package com.planifi.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.domain.Transaction;
import com.planifi.backend.domain.TransactionTag;
import com.planifi.backend.domain.TransactionTagId;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import com.planifi.backend.infrastructure.persistence.TransactionRepository;
import com.planifi.backend.infrastructure.persistence.TransactionTagRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTagRepository transactionTagRepository;
    private final AccountRepository accountRepository;
    private final TagService tagService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionTagRepository transactionTagRepository,
                              AccountRepository accountRepository,
                              TagService tagService,
                              IdempotencyKeyRepository idempotencyKeyRepository,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.accountRepository = accountRepository;
        this.tagService = tagService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResult createTransaction(UUID userId,
                                               UUID accountId,
                                               BigDecimal amount,
                                               LocalDate occurredOn,
                                               String description,
                                               List<String> tags,
                                               boolean createMissingTags,
                                               String idempotencyKey) {
        ensureAccountExists(userId, accountId);
        List<String> normalizedTags = normalizeTags(tags);
        String requestHash = hashRequest(
                "create-transaction",
                userId.toString(),
                accountId.toString(),
                amount.stripTrailingZeros().toPlainString(),
                occurredOn.toString(),
                description,
                Boolean.toString(createMissingTags),
                hashTagsComponent(normalizedTags)
        );

        return executeIdempotent(idempotencyKey, requestHash, TransactionResult.class, () -> {
            Transaction transaction = transactionRepository.save(new Transaction(
                    UUID.randomUUID(),
                    accountId,
                    amount,
                    occurredOn,
                    description,
                    OffsetDateTime.now()
            ));

            List<Tag> resolvedTags = tagService.resolveTags(userId, normalizedTags, createMissingTags);
            if (!resolvedTags.isEmpty()) {
                List<TransactionTag> mappings = resolvedTags.stream()
                        .map(tag -> new TransactionTag(
                                new TransactionTagId(transaction.getId(), tag.getId()),
                                OffsetDateTime.now()
                        ))
                        .toList();
                transactionTagRepository.saveAll(mappings);
            }

            return new TransactionResult(transaction, resolvedTags);
        });
    }

    private void ensureAccountExists(UUID userId, UUID accountId) {
        if (accountRepository.findByIdAndUserId(accountId, userId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        List<String> normalized = tags.stream()
                .map(tag -> tag == null ? "" : tag.trim())
                .filter(tag -> !tag.isEmpty())
                .toList();
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String tag : normalized) {
            String key = tag.toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, tag);
        }
        return List.copyOf(unique.values());
    }

    private String hashTagsComponent(List<String> tags) {
        return tags.stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
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
            if (stored.getResponseBody() == null) {
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
