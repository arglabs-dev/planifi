package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<Tag> findByUserIdOrderByNameAsc(UUID userId);
}
