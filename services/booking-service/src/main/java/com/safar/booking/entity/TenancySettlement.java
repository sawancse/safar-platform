package com.safar.booking.entity;

import com.safar.booking.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tenancy_settlements", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "settlement_ref", unique = true, length = 30)
    private String settlementRef;

    @Column(name = "move_out_date", nullable = false)
    private LocalDate moveOutDate;

    @Column(name = "inspection_date")
    private LocalDate inspectionDate;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "security_deposit_paise", nullable = false)
    private long securityDepositPaise;

    @Column(name = "unpaid_rent_paise", nullable = false)
    @Builder.Default
    private long unpaidRentPaise = 0;

    @Column(name = "unpaid_utilities_paise", nullable = false)
    @Builder.Default
    private long unpaidUtilitiesPaise = 0;

    @Column(name = "damage_deduction_paise", nullable = false)
    @Builder.Default
    private long damageDeductionPaise = 0;

    @Column(name = "late_penalty_paise", nullable = false)
    @Builder.Default
    private long latePenaltyPaise = 0;

    @Column(name = "other_deductions_paise", nullable = false)
    @Builder.Default
    private long otherDeductionsPaise = 0;

    @Column(name = "other_deductions_note", length = 500)
    private String otherDeductionsNote;

    @Column(name = "total_deductions_paise", nullable = false)
    @Builder.Default
    private long totalDeductionsPaise = 0;

    @Column(name = "refund_amount_paise", nullable = false)
    @Builder.Default
    private long refundAmountPaise = 0;

    @Column(name = "additional_due_paise", nullable = false)
    @Builder.Default
    private long additionalDuePaise = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.INITIATED;

    @Column(name = "approved_by_host_at")
    private OffsetDateTime approvedByHostAt;

    @Column(name = "approved_by_tenant_at")
    private OffsetDateTime approvedByTenantAt;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "settlement_pdf_url", length = 500)
    private String settlementPdfUrl;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SettlementDeduction> deductions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
