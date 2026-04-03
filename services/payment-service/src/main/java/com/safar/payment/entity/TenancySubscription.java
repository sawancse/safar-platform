package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenancy_subscriptions", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "razorpay_plan_id", nullable = false, length = 100)
    private String razorpayPlanId;

    @Column(name = "razorpay_subscription_id", nullable = false, unique = true, length = 100)
    private String razorpaySubscriptionId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CREATED";

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "current_start")
    private OffsetDateTime currentStart;

    @Column(name = "current_end")
    private OffsetDateTime currentEnd;

    @Column(name = "charge_attempts", nullable = false)
    @Builder.Default
    private int chargeAttempts = 0;

    @Column(name = "last_charged_at")
    private OffsetDateTime lastChargedAt;

    @Column(name = "last_failed_at")
    private OffsetDateTime lastFailedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
