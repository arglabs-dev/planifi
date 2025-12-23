package com.planifi.backend.application;

import com.planifi.backend.domain.Expense;
import com.planifi.backend.infrastructure.persistence.ExpenseRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    public Expense create(Expense expense) {
        UUID expenseId = expense.getId() == null ? UUID.randomUUID() : expense.getId();
        OffsetDateTime createdAt = expense.getCreatedAt() == null
                ? OffsetDateTime.now()
                : expense.getCreatedAt();

        Expense toPersist = new Expense(
                expenseId,
                expense.getAmount(),
                expense.getOccurredOn(),
                expense.getDescription(),
                createdAt
        );
        return expenseRepository.save(toPersist);
    }
}
