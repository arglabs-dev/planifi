package com.planifi.backend.infrastructure.persistence;

import com.planifi.backend.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByUserIdAndNameIn(UUID userId, Collection<String> names);
}
