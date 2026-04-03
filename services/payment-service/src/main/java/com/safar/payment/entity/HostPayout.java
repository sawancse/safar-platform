package com.safar.payment.entity;

import com.safar.payment.entity.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_payouts", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "gross_amount_paise", nullable = false)
    private long grossAmountPaise;

    @Column(name = "commission_rate_bps", nullable = false)
    private int commissionRateBps;

    @Column(name = "commission_paise", nullable = false)
    private long commissionPaise;

    @Column(name = "gst_on_commission_paise", nullable = false)
    private long gstOnCommissionPaise;

    @Column(name = "tds_amount_paise", nullable = false)
    @Builder.Default
    private long tdsAmountPaise = 0;

    @Column(name = "net_payout_paise", nullable = false)
    private long netPayoutPaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(name = "razorpay_transfer_id", length = 100)
    private String razorpayTransferId;

    @Column(name = "razorpay_payout_id", length = 100)
    private String razorpayPayoutId;

    @Column(name = "payout_date")
    private LocalDate payoutDate;

    @Column(name = "settlement_period_start")
    private LocalDate settlementPeriodStart;

    @Column(name = "settlement_period_end")
    private LocalDate settlementPeriodEnd;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
