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
import com.planifi.backend.infrastructure.persistence.ExpenseRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
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

        CreateExpenseRequest expenseRequest = new CreateExpenseRequest(
                new BigDecimal("45.00"),
                LocalDate.of(2024, 10, 5),
                "Office supplies"
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        CreateExpenseRequest expenseRequest = new CreateExpenseRequest(
                new BigDecimal("18.75"),
                LocalDate.of(2024, 10, 6),
                "Taxi"
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isUnauthorized());
    }
}
