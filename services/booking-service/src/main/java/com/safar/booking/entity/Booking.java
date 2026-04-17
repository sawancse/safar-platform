package com.safar.booking.entity;

import com.safar.booking.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String bookingRef;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private UUID listingId;

    @Column(name = "listing_title")
    private String listingTitle;

    @Column(name = "listing_city")
    private String listingCity;

    @Column(name = "listing_type")
    private String listingType;

    @Column(name = "listing_photo_url", columnDefinition = "TEXT")
    private String listingPhotoUrl;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "listing_address", columnDefinition = "TEXT")
    private String listingAddress;

    @Column(nullable = false)
    private LocalDateTime checkIn;

    @Column(nullable = false)
    private LocalDateTime checkOut;

    @Column(nullable = false)
    private Integer guestsCount;

    @Builder.Default
    private Integer adultsCount = 1;

    @Builder.Default
    private Integer childrenCount = 0;

    @Builder.Default
    private Integer infantsCount = 0;

    @Builder.Default
    private Integer petsCount = 0;

    @Column(name = "rooms_count")
    @Builder.Default
    private Integer roomsCount = 1;

    private Integer nights;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private Long baseAmountPaise;

    @Column(nullable = false)
    private Long insuranceAmountPaise;

    @Column(nullable = false)
    private Long gstAmountPaise;

    @Column(nullable = false)
    private Long totalAmountPaise;

    @Column(nullable = false)
    private Long hostPayoutPaise;

    // Guest details
    private String guestFirstName;
    private String guestLastName;
    private String guestEmail;
    private String guestPhone;

    @Builder.Default
    private String bookingFor = "self";

    @Builder.Default
    private Boolean travelForWork = false;

    @Builder.Default
    private Boolean airportShuttle = false;

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "arrival_time", length = 30)
    private String arrivalTime;

    private String cancellationReason;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime completedAt;

    @Builder.Default
    private Long walletCreditsAppliedPaise = 0L;

    @Builder.Default
    private Boolean isRecurring = false;

    private UUID recurringId;

    private UUID groupBookingId;

    @Column(name = "is_primary_booking")
    @Builder.Default
    private Boolean isPrimaryBooking = true;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String bookingType = "SHORT_TERM";

    private UUID organizationId;

    private UUID caseWorkerId;

    private Long monthlyRatePaise;

    // Room type fields
    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(name = "room_type_name", length = 100)
    private String roomTypeName;

    // Commission model fields
    @Column(name = "host_earnings_paise")
    private Long hostEarningsPaise;

    @Column(name = "platform_fee_paise")
    private Long platformFeePaise;

    @Column(name = "cleaning_fee_paise")
    @Builder.Default
    private Long cleaningFeePaise = 0L;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    // Review tracking fields (set via review.created Kafka event)
    @Column(name = "has_review")
    @Builder.Default
    private Boolean hasReview = false;

    @Column(name = "review_rating")
    private Integer reviewRating;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    // Medical booking fields
    private String procedureName;
    private String hospitalName;
    private UUID hospitalId;
    @Column(length = 100)
    private String specialty;
    private LocalDate procedureDate;
    private Integer hospitalDays;
    private Integer recoveryDays;
    private Long treatmentCostPaise;
    @Column(columnDefinition = "TEXT")
    private String patientNotes;

    // Non-refundable & Pay-at-Property
    @Column(name = "non_refundable")
    @Builder.Default
    private Boolean nonRefundable = false;

    @Column(name = "non_refundable_discount_paise")
    @Builder.Default
    private Long nonRefundableDiscountPaise = 0L;

    @Column(name = "payment_mode", length = 20)
    @Builder.Default
    private String paymentMode = "PREPAID"; // PREPAID, PAY_AT_PROPERTY, PARTIAL_PREPAID

    @Column(name = "prepaid_amount_paise")
    private Long prepaidAmountPaise; // amount charged at booking (null = full amount)

    @Column(name = "due_at_property_paise")
    private Long dueAtPropertyPaise; // remaining amount to pay at check-in

    @Column(name = "cash_collected_paise")
    private Long cashCollectedPaise; // cash amount collected at property

    @Column(name = "cash_collection_note")
    private String cashCollectionNote;

    // Inclusions & Perks
    @Column(name = "inclusions_total_paise")
    @Builder.Default
    private Long inclusionsTotalPaise = 0L;

    // PG/Hotel booking fields
    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "security_deposit_paise")
    private Long securityDepositPaise;

    @Column(name = "security_deposit_status")
    private String securityDepositStatus; // PENDING, COLLECTED, REFUNDED

    @Column(name = "pricing_unit", length = 10)
    @Builder.Default
    private String pricingUnit = "NIGHT"; // NIGHT, MONTH, HOUR

    @Column(name = "lease_duration_months")
    private Integer leaseDurationMonths;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
