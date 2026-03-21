package com.safar.listing.repository;

import com.safar.listing.entity.AashrayCase;
import com.safar.listing.entity.enums.CasePriority;
import com.safar.listing.entity.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AashrayCaseRepository extends JpaRepository<AashrayCase, UUID> {

    Page<AashrayCase> findByStatus(CaseStatus status, Pageable pageable);

    Page<AashrayCase> findByPreferredCity(String city, Pageable pageable);

    Page<AashrayCase> findByStatusAndPreferredCity(CaseStatus status, String city, Pageable pageable);

    Page<AashrayCase> findByPriority(CasePriority priority, Pageable pageable);

    Page<AashrayCase> findByAssignedNgoId(UUID ngoId, Pageable pageable);

    Optional<AashrayCase> findByCaseNumber(String caseNumber);

    long countByStatus(CaseStatus status);

    long countByStatusAndPreferredCity(CaseStatus status, String city);

    @Query("SELECT a.preferredCity, a.status, COUNT(a) FROM AashrayCase a GROUP BY a.preferredCity, a.status")
    java.util.List<Object[]> getCaseStatsByCity();
}
