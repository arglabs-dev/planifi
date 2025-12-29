package com.planifi.backend.application;

import com.planifi.backend.api.dto.CreateTransactionRequest;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.domain.Transaction;
import com.planifi.backend.domain.TransactionTag;
import com.planifi.backend.domain.TransactionTagId;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.TagRepository;
import com.planifi.backend.infrastructure.persistence.TransactionRepository;
import com.planifi.backend.infrastructure.persistence.TransactionTagRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTagRepository transactionTagRepository;
    private final TagRepository tagRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionTagRepository transactionTagRepository,
                              TagRepository tagRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.tagRepository = tagRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Transaction createTransaction(UUID userId, CreateTransactionRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new AccountNotFoundException(request.accountId()));
        List<String> requestedTags = request.tags() == null ? List.of() : request.tags();
        Map<String, Tag> resolvedTags = resolveTags(userId, requestedTags, request.createMissingTags());

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                account.getId(),
                request.amount(),
                request.occurredOn(),
                request.description(),
                OffsetDateTime.now()
        );
        Transaction persisted = transactionRepository.save(transaction);

        if (!requestedTags.isEmpty()) {
            OffsetDateTime createdAt = OffsetDateTime.now();
            List<TransactionTag> associations = requestedTags.stream()
                    .map(tagName -> new TransactionTag(
                            new TransactionTagId(persisted.getId(), resolvedTags.get(tagName).getId()),
                            createdAt))
                    .toList();
            transactionTagRepository.saveAll(associations);
        }

        return persisted;
    }

    private Map<String, Tag> resolveTags(UUID userId, List<String> requestedTags, boolean createMissingTags) {
        if (requestedTags.isEmpty()) {
            return Map.of();
        }

        List<Tag> existingTags = tagRepository.findByUserIdAndNameIn(userId, requestedTags);
        Map<String, Tag> resolvedTags = existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, tag -> tag, (left, right) -> left, LinkedHashMap::new));

        List<String> missingTags = requestedTags.stream()
                .filter(tagName -> !resolvedTags.containsKey(tagName))
                .distinct()
                .toList();

        if (!missingTags.isEmpty() && !createMissingTags) {
            throw new TagNotFoundException(missingTags);
        }

        if (!missingTags.isEmpty()) {
            OffsetDateTime now = OffsetDateTime.now();
            List<Tag> created = new ArrayList<>();
            for (String tagName : missingTags) {
                Tag tag = new Tag(UUID.randomUUID(), userId, tagName, now);
                created.add(tag);
                resolvedTags.put(tagName, tag);
            }
            tagRepository.saveAll(created);
        }

        return resolvedTags;
    }
}
