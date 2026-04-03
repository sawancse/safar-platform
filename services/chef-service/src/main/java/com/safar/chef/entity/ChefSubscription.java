package com.safar.chef.entity;

import com.safar.chef.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_subscriptions", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_ref", unique = true, length = 20)
    private String subscriptionRef;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chef_name")
    private String chefName;

    @Column(name = "customer_name")
    private String customerName;

    private String plan;

    @Column(name = "meals_per_day")
    @Builder.Default
    private Integer mealsPerDay = 1;

    @Column(name = "meal_types")
    private String mealTypes;

    @Column(columnDefinition = "TEXT")
    private String schedule;

    @Column(name = "monthly_rate_paise")
    private Long monthlyRatePaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "chef_earnings_paise")
    private Long chefEarningsPaise;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_renewal_date")
    private LocalDate nextRenewalDate;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String locality;

    private String pincode;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "dietary_preferences")
    private String dietaryPreferences;

    @Column(name = "razorpay_subscription_id")
    private String razorpaySubscriptionId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "modified_at")
    private OffsetDateTime modifiedAt;

    @Column(name = "modification_count")
    @Builder.Default
    private Integer modificationCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
