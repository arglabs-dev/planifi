package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import com.planifi.backend.infrastructure.persistence.TagRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        tagService = new TagService(tagRepository, idempotencyKeyRepository, objectMapper);
    }

    @Test
    void createTagReturnsExistingTagWhenConstraintViolationOccurs() {
        UUID userId = UUID.randomUUID();
        String name = "Travel";
        String idempotencyKey = "idem-123";
        Tag existing = new Tag(UUID.randomUUID(), userId, name, OffsetDateTime.now());

        when(idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(tagRepository.findByUserIdAndNameIgnoreCase(userId, name))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Tag result = tagService.createTag(userId, name, idempotencyKey);

        assertThat(result).isEqualTo(existing);
        verify(tagRepository, times(2)).findByUserIdAndNameIgnoreCase(userId, name);
        verify(tagRepository).saveAndFlush(any(Tag.class));
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void resolveTagsFallsBackToExistingTagWhenConstraintViolationOccurs() {
        UUID userId = UUID.randomUUID();
        String name = "Travel";
        Tag existing = new Tag(UUID.randomUUID(), userId, name, OffsetDateTime.now());

        when(tagRepository.findByUserIdAndNameIgnoreCase(userId, name))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        List<Tag> result = tagService.resolveTags(userId, List.of(name), true);

        assertThat(result).containsExactly(existing);
        verify(tagRepository, times(2)).findByUserIdAndNameIgnoreCase(userId, name);
        verify(tagRepository).saveAndFlush(any(Tag.class));
    }
}
