package com.safar.chef.entity;

import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.entity.enums.MealType;
import com.safar.chef.entity.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_bookings", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", unique = true, length = 20)
    private String bookingRef;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chef_name")
    private String chefName;

    @Column(name = "customer_name")
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType mealType;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "service_time")
    private String serviceTime;

    @Column(name = "guests_count")
    private Integer guestsCount;

    @Column(name = "number_of_meals")
    @Builder.Default
    private Integer numberOfMeals = 1;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "menu_name")
    private String menuName;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String locality;

    private String pincode;

    @Column(name = "total_amount_paise")
    private Long totalAmountPaise;

    @Column(name = "advance_amount_paise")
    @Builder.Default
    private Long advanceAmountPaise = 0L;

    @Column(name = "balance_amount_paise")
    @Builder.Default
    private Long balanceAmountPaise = 0L;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "chef_earnings_paise")
    private Long chefEarningsPaise;

    @Column(name = "advance_paid_paise")
    @Builder.Default
    private Long advancePaidPaise = 0L;

    @Column(name = "payment_adjustment_paise")
    @Builder.Default
    private Long paymentAdjustmentPaise = 0L;

    @Column(name = "previous_total_paise")
    private Long previousTotalPaise;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "payment_status")
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Enumerated(EnumType.STRING)
    private ChefBookingStatus status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "rating_given")
    private Integer ratingGiven;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "auto_expire_at")
    private OffsetDateTime autoExpireAt;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "chef_lat")
    private Double chefLat;

    @Column(name = "chef_lng")
    private Double chefLng;

    @Column(name = "location_updated_at")
    private OffsetDateTime locationUpdatedAt;

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

    @Column(name = "appliances_json", columnDefinition = "TEXT")
    private String appliancesJson;

    @Column(name = "crockery_json", columnDefinition = "TEXT")
    private String crockeryJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
