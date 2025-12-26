package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.api.dto.CreateExpenseRequest;
import com.planifi.backend.domain.Expense;
import com.planifi.backend.infrastructure.persistence.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    private ExpenseRepository expenseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        expenseRepository.deleteAll();
    }

    @Test
    void createExpensePersistsAndReturnsPayload() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("15.50"),
                LocalDate.of(2024, 9, 20),
                "Team lunch"
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(15.50))
                .andExpect(jsonPath("$.occurredOn").value("2024-09-20"))
                .andExpect(jsonPath("$.description").value("Team lunch"))
                .andReturn();

        assertThat(expenseRepository.count()).isEqualTo(1);
        Expense persisted = expenseRepository.findAll().getFirst();
        assertThat(persisted.getAmount()).isEqualByComparingTo("15.50");
        assertThat(persisted.getOccurredOn()).isEqualTo(LocalDate.of(2024, 9, 20));
        assertThat(persisted.getDescription()).isEqualTo("Team lunch");

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(persisted.getId().toString());
    }

    @Test
    void listExpensesReturnsPersistedEntries() throws Exception {
        Expense savedExpense = expenseRepository.save(new Expense(
                UUID.randomUUID(),
                new BigDecimal("99.99"),
                LocalDate.of(2024, 8, 1),
                "Travel stipend",
                OffsetDateTime.parse("2024-08-02T10:15:30+00:00")
        ));

        MvcResult result = mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedExpense.getId().toString()))
                .andExpect(jsonPath("$[0].amount").value(99.99))
                .andExpect(jsonPath("$[0].occurredOn").value("2024-08-01"))
                .andExpect(jsonPath("$[0].description").value("Travel stipend"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String createdAt = objectMapper.readTree(body).get(0).get("createdAt").asText();
        assertThat(OffsetDateTime.parse(createdAt).toInstant())
                .isEqualTo(savedExpense.getCreatedAt().toInstant());
    }
}
