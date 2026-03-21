package com.safar.payment.entity;

import com.safar.payment.entity.enums.RefundStatus;
import com.safar.payment.entity.enums.RefundType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    private UUID bookingId;

    @Column(length = 100)
    private String gatewayRefundId;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 50)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundType refundType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RefundStatus status = RefundStatus.INITIATED;

    private UUID fxLockId;

    @Column(length = 3)
    private String originalCurrency;

    private Long originalAmount;

    @Builder.Default
    private OffsetDateTime initiatedAt = OffsetDateTime.now();

    private OffsetDateTime completedAt;

    @Column(length = 500)
    private String failureReason;
}
