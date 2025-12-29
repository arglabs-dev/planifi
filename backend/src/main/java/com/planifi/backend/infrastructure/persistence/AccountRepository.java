package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findByUserIdAndDisabledAtIsNullOrderByCreatedAtAsc(UUID userId);
}
