package com.planifi.backend.api;

import com.planifi.backend.api.dto.CreateExpenseRequest;
import com.planifi.backend.api.dto.ExpenseResponse;
import com.planifi.backend.application.ExpenseService;
import com.planifi.backend.domain.Expense;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
@Validated
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseResponse> listExpenses() {
        return expenseService.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        Expense newExpense = new Expense(
                UUID.randomUUID(),
                request.amount(),
                request.occurredOn(),
                request.description(),
                OffsetDateTime.now()
        );
        Expense persisted = expenseService.create(newExpense);
        return toResponse(persisted);
    }

    private ExpenseResponse toResponse(Expense expense) {
        BigDecimal amount = expense.getAmount();
        return new ExpenseResponse(
                expense.getId(),
                amount,
                expense.getOccurredOn(),
                expense.getDescription(),
                expense.getCreatedAt()
        );
    }
}
