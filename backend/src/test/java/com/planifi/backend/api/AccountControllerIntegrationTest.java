package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateAccountRequest;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
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
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticatedUser authenticatedUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        userRepository.deleteAll();

        UUID userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "user@planifi.app",
                "password-hash",
                "Test User",
                OffsetDateTime.now()
        ));
        authenticatedUser = new AuthenticatedUser(userId, "user@planifi.app");
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
    }

    @Test
    void createAccountPersistsAndReturnsPayload() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("Cuenta nómina",
                AccountType.BANK);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/accounts")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Cuenta nómina"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currency").value("MXN"));

        assertThat(accountRepository.count()).isEqualTo(1);
        Account saved = accountRepository.findAll().getFirst();
        assertThat(saved.getUserId()).isEqualTo(authenticatedUser.userId());
        assertThat(saved.getType()).isEqualTo(AccountType.BANK);
        assertThat(saved.getDisabledAt()).isNull();
    }

    @Test
    void listAccountsReturnsOnlyActiveEntries() throws Exception {
        Account active = accountRepository.save(new Account(
                UUID.randomUUID(),
                authenticatedUser.userId(),
                "Cuenta principal",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.parse("2024-09-10T12:00:00Z"),
                null
        ));
        accountRepository.save(new Account(
                UUID.randomUUID(),
                authenticatedUser.userId(),
                "Cuenta deshabilitada",
                AccountType.DEBIT_CARD,
                "MXN",
                OffsetDateTime.parse("2024-09-11T12:00:00Z"),
                OffsetDateTime.parse("2024-09-12T12:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/accounts")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(active.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Cuenta principal"))
                .andExpect(jsonPath("$[0].type").value("CASH"))
                .andExpect(jsonPath("$[0].currency").value("MXN"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void disableAccountMarksAsDisabled() throws Exception {
        Account active = accountRepository.save(new Account(
                UUID.randomUUID(),
                authenticatedUser.userId(),
                "Cuenta principal",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.parse("2024-09-10T12:00:00Z"),
                null
        ));

        mockMvc.perform(post("/api/v1/accounts/" + active.getId() + "/disable")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-98765432"))
                .andExpect(status().isNoContent());

        Account updated = accountRepository.findById(active.getId()).orElseThrow();
        assertThat(updated.getDisabledAt()).isNotNull();
    }
}
