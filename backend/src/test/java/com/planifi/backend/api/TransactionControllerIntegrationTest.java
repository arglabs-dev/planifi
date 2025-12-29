package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateTransactionRequest;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.TagRepository;
import com.planifi.backend.infrastructure.persistence.TransactionRepository;
import com.planifi.backend.infrastructure.persistence.TransactionTagRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTagRepository transactionTagRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication authentication;
    private Account account;

    @BeforeEach
    void setUp() {
        transactionTagRepository.deleteAll();
        transactionRepository.deleteAll();
        tagRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        UUID userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "user@planifi.app",
                "password-hash",
                "Test User",
                OffsetDateTime.now()
        ));
        account = accountRepository.save(new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta principal",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.now(),
                null
        ));
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "user@planifi.app");
        authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
    }

    @Test
    void createTransactionRejectsMissingTagsBeforePersisting() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                account.getId(),
                new BigDecimal("42.50"),
                LocalDate.of(2024, 10, 5),
                "Office supplies",
                List.of("MissingTag"),
                false
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-transaction-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TAG_NOT_FOUND"));

        assertThat(transactionRepository.count()).isZero();
        assertThat(transactionTagRepository.count()).isZero();
    }
}
