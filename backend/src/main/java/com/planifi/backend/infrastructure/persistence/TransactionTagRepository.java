package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.TransactionTag;
import com.planifi.backend.domain.TransactionTagId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionTagRepository extends JpaRepository<TransactionTag, TransactionTagId> {
    List<TransactionTag> findByIdTransactionIdIn(List<UUID> transactionIds);
}
