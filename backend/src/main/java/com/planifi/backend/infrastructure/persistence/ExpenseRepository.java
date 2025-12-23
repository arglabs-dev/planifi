package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.Expense;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
}
