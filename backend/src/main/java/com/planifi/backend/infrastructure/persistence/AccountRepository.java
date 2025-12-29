package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserIdAndNameIgnoreCase(UUID userId, String name);
}
