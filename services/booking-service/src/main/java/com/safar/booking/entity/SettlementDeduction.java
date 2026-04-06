package com.safar.booking.entity;

import com.safar.booking.entity.enums.DeductionCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_deductions", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private TenancySettlement settlement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeductionCategory category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    // Dispute fields
    @Column(nullable = false)
    @Builder.Default
    private boolean disputed = false;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "dispute_resolved", nullable = false)
    @Builder.Default
    private boolean disputeResolved = false;

    @Column(name = "admin_decision", length = 20)
    private String adminDecision;

    @Column(name = "admin_adjusted_paise")
    private Long adminAdjustedPaise;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
