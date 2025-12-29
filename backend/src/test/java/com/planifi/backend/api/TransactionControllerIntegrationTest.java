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
import com.planifi.backend.domain.Transaction;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
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
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTagRepository transactionTagRepository;

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
    private Account account;

    @BeforeEach
    void setUp() {
        transactionTagRepository.deleteAll();
        transactionRepository.deleteAll();
        tagRepository.deleteAll();
        accountRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        userRepository.deleteAll();

        userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "transactions@planifi.app",
                "password-hash",
                "Transaction Tester",
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

        authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "transactions@planifi.app"),
                null,
                List.of()
        );
    }

    @Test
    void createTransactionCreatesTagsAndAssociations() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                account.getId(),
                new BigDecimal("120.00"),
                LocalDate.of(2024, 12, 5),
                "Despensa",
                List.of("Super", "Comida"),
                true
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-tx-123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
                .andExpect(jsonPath("$.tags[0].name").value("Super"))
                .andExpect(jsonPath("$.tags[1].name").value("Comida"));

        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(2);
        assertThat(transactionTagRepository.count()).isEqualTo(2);

        Transaction saved = transactionRepository.findAll().getFirst();
        assertThat(saved.getAccountId()).isEqualTo(account.getId());
    }
}
