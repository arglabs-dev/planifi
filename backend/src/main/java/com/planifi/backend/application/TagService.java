package com.planifi.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import com.planifi.backend.infrastructure.persistence.TagRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public TagService(TagRepository tagRepository,
                      IdempotencyKeyRepository idempotencyKeyRepository,
                      ObjectMapper objectMapper) {
        this.tagRepository = tagRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Tag> listTags(UUID userId) {
        return tagRepository.findByUserIdOrderByNameAsc(userId);
    }

    @Transactional
    public Tag createTag(UUID userId, String name, String idempotencyKey) {
        String normalized = normalize(name);
        String requestHash = hashRequest("create-tag", userId.toString(), normalized.toLowerCase(Locale.ROOT));
        return executeIdempotent(idempotencyKey, requestHash, Tag.class,
                () -> findOrCreate(userId, normalized));
    }

    @Transactional
    public List<Tag> resolveTags(UUID userId, List<String> names, boolean createMissing) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        Map<String, Tag> resolved = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            String normalized = normalize(name);
            String key = normalized.toLowerCase(Locale.ROOT);
            if (resolved.containsKey(key)) {
                continue;
            }
            Optional<Tag> existing = tagRepository.findByUserIdAndNameIgnoreCase(userId, normalized);
            if (existing.isPresent()) {
                resolved.put(key, existing.get());
                continue;
            }
            if (!createMissing) {
                missing.add(normalized);
                continue;
            }
            Tag created;
            try {
                created = tagRepository.save(new Tag(
                        UUID.randomUUID(),
                        userId,
                        normalized,
                        OffsetDateTime.now()
                ));
            } catch (DataIntegrityViolationException ex) {
                created = tagRepository.findByUserIdAndNameIgnoreCase(userId, normalized)
                        .orElseThrow(() -> ex);
            }
            resolved.put(key, created);
        }
        if (!missing.isEmpty()) {
            throw new TagNotFoundException(missing);
        }
        return List.copyOf(resolved.values());
    }

    private Tag findOrCreate(UUID userId, String normalizedName) {
        return tagRepository.findByUserIdAndNameIgnoreCase(userId, normalizedName)
                .orElseGet(() -> {
                    try {
                        return tagRepository.save(new Tag(
                                UUID.randomUUID(),
                                userId,
                                normalizedName,
                                OffsetDateTime.now()
                        ));
                    } catch (DataIntegrityViolationException ex) {
                        return tagRepository.findByUserIdAndNameIgnoreCase(userId, normalizedName)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim();
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
