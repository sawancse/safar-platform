package com.safar.flight.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Refund-pending-admin-approval queue entry. Created by FlightBookingService.cancelBooking()
 * when refund amount exceeds the auto-confirm threshold (₹10k) per Tree-4 of the design.
 *
 * Lifecycle: PENDING → APPROVED (admin) → COMPLETED (refund initiated)
 *           PENDING → REJECTED (admin)
 */
@Entity
@Table(name = "refund_approvals", schema = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID flightBookingId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long requestedAmountPaise;

    private Long approvedAmountPaise;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";       // PENDING / APPROVED / REJECTED / COMPLETED

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL";      // NORMAL / HIGH (4hr SLA)

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 20)
    private String fareRule;                 // REFUNDABLE / PARTIAL / NON_REFUNDABLE

    @Column(nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    private Instant reviewedAt;
    private UUID reviewedByUserId;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    private Instant completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
