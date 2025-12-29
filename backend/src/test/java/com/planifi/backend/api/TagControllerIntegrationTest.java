package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateTagRequest;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Tag;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import com.planifi.backend.infrastructure.persistence.TagRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication authentication;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        userRepository.deleteAll();

        userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "tags@planifi.app",
                "password-hash",
                "Tag Tester",
                OffsetDateTime.now()
        ));
        authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "tags@planifi.app"),
                null,
                List.of()
        );
    }

    @Test
    void createTagPersistsAndReturnsPayload() throws Exception {
        CreateTagRequest request = new CreateTagRequest("Supermercado");

        mockMvc.perform(post("/api/v1/tags")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-tag-123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Supermercado"));

        assertThat(tagRepository.count()).isEqualTo(1);
        Tag stored = tagRepository.findAll().getFirst();
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.getName()).isEqualTo("Supermercado");
    }

    @Test
    void listTagsReturnsOrderedEntries() throws Exception {
        tagRepository.save(new Tag(
                UUID.randomUUID(),
                userId,
                "Zeta",
                OffsetDateTime.parse("2024-01-10T10:00:00Z")
        ));
        tagRepository.save(new Tag(
                UUID.randomUUID(),
                userId,
                "Alpha",
                OffsetDateTime.parse("2024-01-11T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/tags")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].name").value("Zeta"));
    }
}
