package com.safar.payment.entity;

import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_lines", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SettlementPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecipientType recipientType;

    private UUID recipientId;

    @Column(nullable = false)
    private Long amountPaise;

    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(length = 20)
    private String payoutMethod;

    private OffsetDateTime scheduledAt;
    private OffsetDateTime completedAt;

    @Column(length = 500)
    private String failureReason;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
