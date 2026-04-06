package com.safar.booking.repository;

import com.safar.booking.entity.TenancySettlement;
import com.safar.booking.entity.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenancySettlementRepository extends JpaRepository<TenancySettlement, UUID> {

    Optional<TenancySettlement> findByTenancyId(UUID tenancyId);

    Optional<TenancySettlement> findBySettlementRef(String settlementRef);

    List<TenancySettlement> findByStatus(SettlementStatus status);

    // Overdue detection
    List<TenancySettlement> findByIsOverdueFalseAndRefundDeadlineDateBeforeAndStatusIn(
            LocalDate date, List<SettlementStatus> statuses);

    // Admin: paginated queries
    Page<TenancySettlement> findByStatusInOrderByCreatedAtDesc(
            List<SettlementStatus> statuses, Pageable pageable);

    Page<TenancySettlement> findByIsOverdueTrueOrderByRefundDeadlineDateAsc(Pageable pageable);

    Page<TenancySettlement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(SettlementStatus status);

    long countByIsOverdueTrue();
}
