package com.planifi.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.LoginRequest;
import com.planifi.backend.api.dto.RegisterUserRequest;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
        "planifi.security.rate-limit.sensitive-paths=/api/v1/auth/login"
})
class AuthRateLimitIntegrationTest {

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
    void rateLimitBlocksExcessAuthRequests() throws Exception {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "rate-limit@example.com",
                "Sup3rS3cret!",
                "Rate Limit"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("rate-limit@example.com", "Sup3rS3cret!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }
}
