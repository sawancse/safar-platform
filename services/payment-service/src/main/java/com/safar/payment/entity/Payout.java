package com.safar.payment.entity;

import com.safar.payment.entity.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payouts", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID hostId;

    private UUID bookingId;

    @Column(nullable = false)
    private Long amountPaise;

    @Builder.Default
    private Long tdsPaise = 0L;

    @Column(nullable = false)
    private Long netAmountPaise;

    @Column(nullable = false, length = 20)
    private String method;

    private String upiId;
    private String razorpayPayoutId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    private String scheduledBatch;  // e.g., "2026-03-15-0600"

    private OffsetDateTime scheduledAt;

    @Builder.Default
    private Integer retryCount = 0;

    @Column(length = 500)
    private String failureReason;

    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
