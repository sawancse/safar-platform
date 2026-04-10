package com.safar.user.repository;

import com.safar.user.entity.LeadActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
    Page<LeadActivity> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);
    long countByActivityTypeAndCreatedAtAfter(String activityType, OffsetDateTime after);
}
