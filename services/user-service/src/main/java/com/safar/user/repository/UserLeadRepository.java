package com.safar.user.repository;

import com.safar.user.entity.UserLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLeadRepository extends JpaRepository<UserLead, UUID> {

    Optional<UserLead> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<UserLead> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserLead> findAllByOrderByLeadScoreDesc(Pageable pageable);

    Page<UserLead> findByCityIgnoreCaseOrderByCreatedAtDesc(String city, Pageable pageable);

    long countByCreatedAtAfter(OffsetDateTime after);

    long countByConvertedTrue();

    // Segment queries
    List<UserLead> findBySegment(String segment);

    // Nurture queries
    List<UserLead> findByNurtureDay0SentFalseAndSubscribedTrueAndConvertedFalse();

    List<UserLead> findByNurtureDay3SentFalseAndNurtureDay0SentTrueAndSubscribedTrueAndConvertedFalseAndCreatedAtBefore(OffsetDateTime before);

    List<UserLead> findByNurtureDay7SentFalseAndNurtureDay3SentTrueAndSubscribedTrueAndConvertedFalseAndCreatedAtBefore(OffsetDateTime before);

    // Re-engagement
    List<UserLead> findBySubscribedTrueAndConvertedFalseAndLastActiveAtBefore(OffsetDateTime before);

    // Stats
    long countBySegment(String segment);

    @org.springframework.data.jpa.repository.Query("SELECT l.segment, COUNT(l) FROM UserLead l GROUP BY l.segment")
    List<Object[]> countBySegmentGrouped();

    @org.springframework.data.jpa.repository.Query("SELECT l.source, COUNT(l) FROM UserLead l GROUP BY l.source")
    List<Object[]> countBySourceGrouped();

    @org.springframework.data.jpa.repository.Query("SELECT l.source, COUNT(l) FROM UserLead l WHERE l.converted = true GROUP BY l.source")
    List<Object[]> countConvertedBySourceGrouped();
}
