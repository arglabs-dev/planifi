package com.planifi.backend.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateExpenseRequest;
import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "planifi.security.enabled=true",
        "planifi.security.jwt.secret=test-jwt-secret-test-jwt-secret-test-jwt-secret-123456",
        "planifi.security.jwt.issuer=planifi-backend-test",
        "planifi.security.jwt.expiration-minutes=60",
        "planifi.security.rate-limit.enabled=true",
        "planifi.security.rate-limit.requests-per-minute=1",
        "planifi.security.rate-limit.burst=0",
        "planifi.security.rate-limit.sensitive-paths=/api/v1/expenses"
})
class ApiKeyRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private UUID userId;
    private Account account;
    private String apiKey;

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
                "rate-limit@planifi.app",
                "password-hash",
                "Rate Limit Tester",
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
        apiKey = "pln_test_" + UUID.randomUUID();
    }

    @Test
    void rateLimitBucketsByApiKeyAcrossIps() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedApiKey(UUID.randomUUID(), userId),
                apiKey,
                List.of()
        );
        String payload = objectMapper.writeValueAsString(expenseRequest());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .with(remoteAddress("10.0.0.1"))
                        .header("X-MCP-API-Key", apiKey)
                        .header("Idempotency-Key", "idem-api-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .with(remoteAddress("10.0.0.2"))
                        .header("X-MCP-API-Key", apiKey)
                        .header("Idempotency-Key", "idem-api-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void rateLimitBucketsByUserIdAcrossIps() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "rate-limit@planifi.app"),
                null,
                List.of()
        );
        String payload = objectMapper.writeValueAsString(expenseRequest());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .with(remoteAddress("10.0.1.1"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test.jwt.token")
                        .header("Idempotency-Key", "idem-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(authentication(authentication))
                        .with(remoteAddress("10.0.1.2"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test.jwt.token")
                        .header("Idempotency-Key", "idem-user-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    private CreateExpenseRequest expenseRequest() {
        return new CreateExpenseRequest(
                account.getId(),
                new BigDecimal("12.50"),
                LocalDate.of(2024, 10, 10),
                "Rate limit test",
                List.of(),
                false
        );
    }

    private RequestPostProcessor remoteAddress(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
