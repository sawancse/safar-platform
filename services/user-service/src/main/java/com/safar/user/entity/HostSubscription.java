package com.safar.user.entity;

import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_subscriptions", schema = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false, unique = true)
    private UUID hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle;

    @Column(name = "razorpay_sub_id")
    private String razorpaySubId;

    @Column(name = "amount_paise", nullable = false)
    private Integer amountPaise;

    @Column(name = "next_billing_at")
    private OffsetDateTime nextBillingAt;

    @Column(name = "commission_discount_percent")
    @Builder.Default
    private Integer commissionDiscountPercent = 0;

    @Column(name = "preferred_partner")
    @Builder.Default
    private Boolean preferredPartner = false;

    @Column(name = "avg_host_rating")
    private java.math.BigDecimal avgHostRating;

    @Column(name = "performance_updated_at")
    private OffsetDateTime performanceUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
