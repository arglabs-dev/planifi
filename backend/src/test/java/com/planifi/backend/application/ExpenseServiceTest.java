package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planifi.backend.domain.Expense;
import com.planifi.backend.infrastructure.persistence.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository);
    }

    @Test
    void createAssignsIdsAndTimestampsWhenMissing() {
        when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Expense expenseWithoutIds = new Expense(
                null,
                new BigDecimal("125.90"),
                LocalDate.of(2024, 10, 3),
                "Conference ticket",
                null
        );

        Expense savedExpense = expenseService.create(expenseWithoutIds);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense persisted = captor.getValue();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getAmount()).isEqualByComparingTo("125.90");
        assertThat(persisted.getOccurredOn()).isEqualTo(LocalDate.of(2024, 10, 3));
        assertThat(persisted.getDescription()).isEqualTo("Conference ticket");

        OffsetDateTime now = OffsetDateTime.now();
        assertThat(savedExpense.getId()).isEqualTo(persisted.getId());
        assertThat(savedExpense.getCreatedAt())
                .isAfterOrEqualTo(now.minusSeconds(5))
                .isBeforeOrEqualTo(now.plusSeconds(5));
    }
}
