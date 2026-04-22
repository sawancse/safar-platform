package com.safar.chef.entity;

import com.safar.chef.entity.enums.EventBookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_bookings", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", unique = true, length = 20)
    private String bookingRef;

    @Column(name = "chef_id")
    private UUID chefId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chef_name")
    private String chefName;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private String eventTime;

    @Column(name = "duration_hours")
    @Builder.Default
    private Integer durationHours = 4;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "venue_address", columnDefinition = "TEXT")
    private String venueAddress;

    private String city;

    private String locality;

    private String pincode;

    @Column(name = "menu_package_id")
    private UUID menuPackageId;

    @Column(name = "menu_description", columnDefinition = "TEXT")
    private String menuDescription;

    @Column(name = "cuisine_preferences")
    private String cuisinePreferences;

    @Column(name = "price_per_plate_paise")
    private Long pricePerPlatePaise;

    @Column(name = "total_food_paise")
    private Long totalFoodPaise;

    @Column(name = "decoration_paise")
    @Builder.Default
    private Long decorationPaise = 0L;

    @Column(name = "cake_paise")
    @Builder.Default
    private Long cakePaise = 0L;

    @Column(name = "staff_paise")
    @Builder.Default
    private Long staffPaise = 0L;

    @Column(name = "other_addons_paise")
    @Builder.Default
    private Long otherAddonsPaise = 0L;

    @Column(name = "total_amount_paise")
    private Long totalAmountPaise;

    @Column(name = "advance_amount_paise")
    private Long advanceAmountPaise;

    @Column(name = "balance_amount_paise")
    private Long balanceAmountPaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "chef_earnings_paise")
    private Long chefEarningsPaise;

    @Column(name = "addons_json", columnDefinition = "TEXT")
    private String addonsJson;

    @Column(name = "services_json", columnDefinition = "TEXT")
    private String servicesJson;

    @Column(name = "staff_roles_json", columnDefinition = "TEXT")
    private String staffRolesJson;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    private EventBookingStatus status;

    @Column(name = "quoted_at")
    private OffsetDateTime quotedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "rating_given")
    private Integer ratingGiven;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @Column(name = "modified_at")
    private OffsetDateTime modifiedAt;

    @Column(name = "modification_count")
    @Builder.Default
    private Integer modificationCount = 0;

    @Column(name = "start_job_otp", length = 6)
    private String startJobOtp;

    @Column(name = "job_started_at")
    private OffsetDateTime jobStartedAt;

    @Column(name = "balance_paid_at")
    private OffsetDateTime balancePaidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
