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
import com.planifi.backend.infrastructure.persistence.TagRepository;
import com.planifi.backend.infrastructure.persistence.TransactionRepository;
import com.planifi.backend.infrastructure.persistence.TransactionTagRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTagRepository transactionTagRepository;
    private final AccountRepository accountRepository;
    private final TagService tagService;
    private final TagRepository tagRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionTagRepository transactionTagRepository,
                              AccountRepository accountRepository,
                              TagService tagService,
                              TagRepository tagRepository,
                              IdempotencyKeyRepository idempotencyKeyRepository,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.accountRepository = accountRepository;
        this.tagService = tagService;
        this.tagRepository = tagRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TransactionResult> listTransactions(UUID userId) {
        List<UUID> accountIds = accountRepository
                .findByUserIdAndDisabledAtIsNullOrderByCreatedAtAsc(userId)
                .stream()
                .map(account -> account.getId())
                .toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        List<Transaction> transactions = transactionRepository
                .findByAccountIdInOrderByOccurredOnDesc(accountIds);
        if (transactions.isEmpty()) {
            return List.of();
        }
        List<UUID> transactionIds = transactions.stream()
                .map(Transaction::getId)
                .toList();
        List<TransactionTag> mappings = transactionTagRepository
                .findByIdTransactionIdIn(transactionIds);
        Set<UUID> tagIds = mappings.stream()
                .map(mapping -> mapping.getId().getTagId())
                .collect(Collectors.toSet());
        Map<UUID, Tag> tagById = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));
        Map<UUID, List<Tag>> tagsByTransactionId = new HashMap<>();
        for (TransactionTag mapping : mappings) {
            Tag tag = tagById.get(mapping.getId().getTagId());
            if (tag == null) {
                continue;
            }
            tagsByTransactionId
                    .computeIfAbsent(mapping.getId().getTransactionId(), ignored -> new ArrayList<>())
                    .add(tag);
        }
        return transactions.stream()
                .map(transaction -> {
                    List<Tag> tags = tagsByTransactionId
                            .getOrDefault(transaction.getId(), List.of())
                            .stream()
                            .sorted(Comparator.comparing(Tag::getName))
                            .toList();
                    return new TransactionResult(transaction, tags);
                })
                .toList();
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
            ensureAccountExists(userId, accountId);
            List<Tag> resolvedTags = tagService.resolveTags(userId, normalizedTags, createMissingTags);
            Transaction transaction = transactionRepository.save(new Transaction(
                    UUID.randomUUID(),
                    accountId,
                    amount,
                    occurredOn,
                    description,
                    OffsetDateTime.now()
            ));
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
            if (responseType == Void.class) {
                return null;
            }
            if (stored.getResponseBody() == null) {
                throw new IllegalStateException("Missing idempotent response body");
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
