package com.safar.listing.repository;

import com.safar.listing.entity.ExperienceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperienceSessionRepository extends JpaRepository<ExperienceSession, UUID> {
    List<ExperienceSession> findByExperienceId(UUID experienceId);
    List<ExperienceSession> findByExperienceIdAndSessionDateGreaterThanEqualAndStatusOrderBySessionDateAscStartTimeAsc(
            UUID experienceId, java.time.LocalDate fromDate, com.safar.listing.entity.enums.SessionStatus status);
}
