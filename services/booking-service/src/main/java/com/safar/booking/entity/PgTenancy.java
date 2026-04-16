package com.safar.booking.entity;

import com.safar.booking.entity.enums.TenancyStatus;
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
@Table(name = "pg_tenancies", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgTenancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_ref", nullable = false, unique = true, length = 20)
    private String tenancyRef;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(name = "bed_number", length = 10)
    private String bedNumber;

    @Column(name = "sharing_type", nullable = false, length = 30)
    @Builder.Default
    private String sharingType = "PRIVATE";

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "notice_period_days", nullable = false)
    @Builder.Default
    private int noticePeriodDays = 30;

    @Column(name = "monthly_rent_paise", nullable = false)
    private long monthlyRentPaise;

    @Column(name = "security_deposit_paise", nullable = false)
    @Builder.Default
    private long securityDepositPaise = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenancyStatus status = TenancyStatus.ACTIVE;

    @Column(name = "meals_included", nullable = false)
    @Builder.Default
    private boolean mealsIncluded = false;

    @Column(name = "laundry_included", nullable = false)
    @Builder.Default
    private boolean laundryIncluded = false;

    @Column(name = "wifi_included", nullable = false)
    @Builder.Default
    private boolean wifiIncluded = false;

    @Column(name = "total_monthly_paise", nullable = false)
    private long totalMonthlyPaise;

    @Column(name = "billing_day", nullable = false)
    @Builder.Default
    private int billingDay = 1;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "razorpay_subscription_id", length = 100)
    private String razorpaySubscriptionId;

    @Column(name = "razorpay_plan_id", length = 100)
    private String razorpayPlanId;

    @Column(name = "subscription_status", length = 20)
    private String subscriptionStatus;

    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private int gracePeriodDays = 5;

    @Column(name = "late_penalty_bps", nullable = false)
    @Builder.Default
    private int latePenaltyBps = 200;

    /** Maximum penalty as a percentage of invoice total (e.g. 25 = 25%). 0 means no cap. */
    @Column(name = "max_penalty_percent", nullable = false)
    @Builder.Default
    private int maxPenaltyPercent = 25;

    /** Whether 7-day advance rent reminder was sent for the current billing cycle. Reset after invoice generation. */
    @Column(name = "rent_advance_reminder_sent", nullable = false)
    @Builder.Default
    private boolean rentAdvanceReminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
