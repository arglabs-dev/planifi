package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateExpenseRequest;
import com.planifi.backend.api.dto.ExpenseResponse;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.domain.Transaction;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.ExpenseRepository;
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
import org.hamcrest.Matchers;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTagRepository transactionTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private AccountRepository accountRepository;

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
    void cleanDatabase() {
        transactionTagRepository.deleteAll();
        transactionRepository.deleteAll();
        tagRepository.deleteAll();
        expenseRepository.deleteAll();
        accountRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        userRepository.deleteAll();

        userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "expenses@planifi.app",
                "password-hash",
                "Expense Tester",
                OffsetDateTime.now()
        ));
        account = accountRepository.save(new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta gastos",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.now(),
                null
        ));
        authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "expenses@planifi.app"),
                null,
                List.of()
        );
    }

    @Test
    void createExpensePersistsAndReturnsPayload() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                account.getId(),
                new BigDecimal("15.50"),
                LocalDate.of(2024, 9, 20),
                "Team lunch",
                List.of("Equipo", "Comida"),
                true
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-expense-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
                .andExpect(jsonPath("$.amount").value(15.50))
                .andExpect(jsonPath("$.occurredOn").value("2024-09-20"))
                .andExpect(jsonPath("$.description").value("Team lunch"))
                .andExpect(jsonPath("$.tags[0].name").value("Equipo"))
                .andReturn();

        assertThat(transactionRepository.count()).isEqualTo(1);
        Transaction persisted = transactionRepository.findAll().getFirst();
        assertThat(persisted.getAmount()).isEqualByComparingTo("15.50");
        assertThat(persisted.getOccurredOn()).isEqualTo(LocalDate.of(2024, 9, 20));
        assertThat(persisted.getDescription()).isEqualTo("Team lunch");

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(persisted.getId().toString());
    }

    @Test
    void createExpenseIsIdempotent() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                account.getId(),
                new BigDecimal("75.00"),
                LocalDate.of(2024, 9, 21),
                "Meal",
                List.of("Comida"),
                true
        );

        MvcResult firstResult = mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-expense-repeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ExpenseResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                ExpenseResponse.class);

        MvcResult secondResult = mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .header("Idempotency-Key", "idem-expense-repeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ExpenseResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                ExpenseResponse.class);

        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(transactionTagRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void listExpensesReturnsPersistedEntries() throws Exception {
        Transaction savedTransaction = transactionRepository.save(new Transaction(
                UUID.randomUUID(),
                account.getId(),
                new BigDecimal("99.99"),
                LocalDate.of(2024, 8, 1),
                "Travel stipend",
                OffsetDateTime.parse("2024-08-02T10:15:30+00:00")
        ));

        MvcResult result = mockMvc.perform(get("/api/v1/expenses")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$[0].accountId").value(account.getId().toString()))
                .andExpect(jsonPath("$[0].amount").value(99.99))
                .andExpect(jsonPath("$[0].occurredOn").value("2024-08-01"))
                .andExpect(jsonPath("$[0].description").value("Travel stipend"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String createdAt = objectMapper.readTree(body).get(0).get("createdAt").asText();
        assertThat(OffsetDateTime.parse(createdAt).toInstant())
                .isEqualTo(savedTransaction.getCreatedAt().toInstant());
    }

    @Test
    void listExpensesIncludesLegacyEntries() throws Exception {
        expenseRepository.save(new com.planifi.backend.domain.Expense(
                UUID.randomUUID(),
                new BigDecimal("12.34"),
                LocalDate.of(2024, 7, 15),
                "Legacy entry",
                OffsetDateTime.parse("2024-07-16T08:30:00+00:00")
        ));

        mockMvc.perform(get("/api/v1/expenses")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(12.34))
                .andExpect(jsonPath("$[0].description").value("Legacy entry"))
                .andExpect(jsonPath("$[0].accountId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[0].tags").isArray());
    }
}
