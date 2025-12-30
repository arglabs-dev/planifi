package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateExpenseRequest;
import com.planifi.backend.api.dto.LoginRequest;
import com.planifi.backend.api.dto.RegisterUserRequest;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "planifi.security.enabled=true",
        "planifi.security.jwt.secret=test-jwt-secret-test-jwt-secret-test-jwt-secret-123456",
        "planifi.security.jwt.issuer=planifi-backend-test",
        "planifi.security.jwt.expiration-minutes=60"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void cleanDatabase() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsTokenPayload() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "maria@example.com",
                "Sup3rS3cret!",
                "Maria Lopez"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("expiresAt").asText()).isNotBlank();
    }

    @Test
    void loginIssuesTokenAndAllowsJwtAccess() throws Exception {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "carlos@example.com",
                "Sup3rS3cret!",
                "Carlos Ramos"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("carlos@example.com", "Sup3rS3cret!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        UUID userId = userRepository.findAll().getFirst().getId();
        Account account = accountRepository.save(new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta autenticada",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.now(),
                null
        ));

        CreateExpenseRequest expenseRequest = new CreateExpenseRequest(
                account.getId(),
                new BigDecimal("45.00"),
                LocalDate.of(2024, 10, 5),
                "Office supplies",
                List.of("Oficina"),
                true
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "idem-auth-expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        CreateExpenseRequest expenseRequest = new CreateExpenseRequest(
                UUID.randomUUID(),
                new BigDecimal("18.75"),
                LocalDate.of(2024, 10, 6),
                "Taxi",
                List.of(),
                false
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isUnauthorized());
    }

}
