package com.safar.services.entity;

import com.safar.services.entity.enums.VendorAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_booking_vendor", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventBookingVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_booking_id", nullable = false)
    private UUID eventBookingId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VendorAssignmentStatus status = VendorAssignmentStatus.ASSIGNED;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "payout_paise")
    private Long payoutPaise;

    @Column(name = "payout_status", length = 20)
    @Builder.Default
    private String payoutStatus = "PENDING";

    @Column(name = "payout_ref", length = 60)
    private String payoutRef;

    @Column(name = "payout_at")
    private OffsetDateTime payoutAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
