package com.safar.listing.repository;

import com.safar.listing.entity.Experience;
import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {
    Page<Experience> findByCityAndCategoryAndStatus(String city, ExperienceCategory category,
                                                     ExperienceStatus status, Pageable pageable);
    Page<Experience> findByCityAndStatus(String city, ExperienceStatus status, Pageable pageable);
    Page<Experience> findByCategoryAndStatus(ExperienceCategory category, ExperienceStatus status, Pageable pageable);
    Page<Experience> findByStatus(ExperienceStatus status, Pageable pageable);
    List<Experience> findByHostId(UUID hostId);
}
