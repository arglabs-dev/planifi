package com.planifi.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PlanifiBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void actuatorHealthIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void expensesEndpointReturnsEmptyList() throws Exception {
        UUID userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                "empty-expenses@planifi.app",
                "password-hash",
                "Empty Expenses",
                OffsetDateTime.now()
        ));
        accountRepository.save(new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta vacía",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.now(),
                null
        ));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "empty-expenses@planifi.app"),
                null,
                List.of()
        );

        MvcResult result = mockMvc.perform(get("/api/v1/expenses")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("[");
    }
}
