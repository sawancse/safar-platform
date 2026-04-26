package com.safar.flight.repository;

import com.safar.flight.entity.RefundApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundApprovalRepository extends JpaRepository<RefundApproval, UUID> {

    /** Admin queue — sorted by priority (HIGH first), then oldest first. */
    Page<RefundApproval> findByStatusOrderByPriorityAscRequestedAtAsc(String status, Pageable pageable);

    List<RefundApproval> findByFlightBookingIdOrderByRequestedAtDesc(UUID flightBookingId);

    long countByStatus(String status);
}
