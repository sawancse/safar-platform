package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listings", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_category")
    private CommercialCategory commercialCategory;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    private Integer bedrooms;
    private Integer bathrooms;

    @Column(name = "total_rooms")
    @Builder.Default
    private Integer totalRooms = 1;

    @Column(name = "amenities", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    //@Convert(converter = com.safar.listing.config.ListToStringConverter.class)
    private List<String> amenities;

    @Column(name = "base_price_paise", nullable = false)
    private Long basePricePaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false)
    @Builder.Default
    private PricingUnit pricingUnit = PricingUnit.NIGHT;

    @Column(name = "min_booking_hours")
    @Builder.Default
    private Integer minBookingHours = 1;

    @Column(name = "ai_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean aiPricingEnabled = false;

    @Column(name = "ai_pricing_min_paise")
    private Long aiPricingMinPaise;

    @Column(name = "ai_pricing_max_paise")
    private Long aiPricingMaxPaise;

    @Column(name = "instant_book", nullable = false)
    @Builder.Default
    private Boolean instantBook = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "august_lock_id")
    private String augustLockId;

    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "gst_applicable", nullable = false)
    @Builder.Default
    private Boolean gstApplicable = true;

    private String gstin;

    // S24: Safety score fields
    @Column(name = "safety_score", precision = 4, scale = 1)
    private BigDecimal safetyScore;

    @Column(name = "safety_label", length = 20)
    private String safetyLabel;

    @Column(name = "women_friendly")
    @Builder.Default
    private Boolean womenFriendly = false;

    @Column(name = "safety_updated_at")
    private OffsetDateTime safetyUpdatedAt;

    // S25: Remote work certification fields
    @Column(name = "rw_certified")
    @Builder.Default
    private Boolean rwCertified = false;

    @Column(name = "rw_certified_at")
    private OffsetDateTime rwCertifiedAt;

    // S44: Medical tourism
    @Column(name = "medical_stay")
    @Builder.Default
    private Boolean medicalStay = false;

    @Column(name = "pet_friendly")
    @Builder.Default
    private Boolean petFriendly = false;

    @Column(name = "max_pets")
    @Builder.Default
    private Integer maxPets = 0;

    // V17: Search filter fields
    @Column(name = "star_rating")
    private Integer starRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy")
    @Builder.Default
    private CancellationPolicy cancellationPolicy = CancellationPolicy.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_plan")
    @Builder.Default
    private MealPlan mealPlan = MealPlan.NONE;

    @Column(name = "bed_types", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> bedTypes;

    @Column(name = "accessibility_features", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> accessibilityFeatures;

    @Column(name = "free_cancellation")
    @Builder.Default
    private Boolean freeCancellation = false;

    @Column(name = "no_prepayment")
    @Builder.Default
    private Boolean noPrepayment = false;

    // V18: House rules & services
    @Column(name = "check_in_from")
    private java.time.LocalTime checkInFrom;

    @Column(name = "check_in_until")
    private java.time.LocalTime checkInUntil;

    @Column(name = "check_out_from")
    private java.time.LocalTime checkOutFrom;

    @Column(name = "check_out_until")
    private java.time.LocalTime checkOutUntil;

    @Column(name = "children_allowed")
    @Builder.Default
    private Boolean childrenAllowed = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "parking_type")
    @Builder.Default
    private ParkingType parkingType = ParkingType.NONE;

    @Column(name = "breakfast_included")
    @Builder.Default
    private Boolean breakfastIncluded = false;

    // V22: Aashray (refugee housing) fields
    @Column(name = "aashray_ready")
    @Builder.Default
    private Boolean aashrayReady = false;

    @Column(name = "aashray_discount_percent")
    @Builder.Default
    private Integer aashrayDiscountPercent = 0;

    @Column(name = "long_term_monthly_paise")
    private Long longTermMonthlyPaise;

    @Column(name = "min_stay_days")
    @Builder.Default
    private Integer minStayDays = 1;

    // V20: Commercial fields
    @Column(name = "area_sqft")
    private Integer areaSqft;

    @Column(name = "operating_hours_from")
    private java.time.LocalTime operatingHoursFrom;

    @Column(name = "operating_hours_until")
    private java.time.LocalTime operatingHoursUntil;

    // V24: Special media URLs
    @Column(name = "floor_plan_url", length = 500)
    private String floorPlanUrl;

    @Column(name = "panorama_url", length = 500)
    private String panoramaUrl;

    @Column(name = "video_tour_url", length = 500)
    private String videoTourUrl;

    @Column(name = "neighborhood_photo_urls", columnDefinition = "TEXT")
    private String neighborhoodPhotoUrls; // comma-separated

    // V27: Discount rules
    @Column(name = "weekly_discount_percent")
    private Integer weeklyDiscountPercent; // e.g., 10 for 10% off 7+ nights

    @Column(name = "monthly_discount_percent")
    private Integer monthlyDiscountPercent; // e.g., 20 for 20% off 28+ nights

    // V33: Cleaning fee
    @Column(name = "cleaning_fee_paise")
    @Builder.Default
    private Long cleaningFeePaise = 0L;

    // Visibility boost (0, 3, or 5 percent)
    @Column(name = "visibility_boost_percent")
    @Builder.Default
    private Integer visibilityBoostPercent = 0;

    // V25: Community verification
    @Column(name = "community_verified")
    @Builder.Default
    private Boolean communityVerified = false;

    @Column(name = "community_verify_count")
    @Builder.Default
    private Integer communityVerifyCount = 0;

    // PG/Co-living specific
    @Column(name = "occupancy_type")
    private String occupancyType; // MALE, FEMALE, COED

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy")
    private GenderPolicy genderPolicy;

    @Column(name = "food_type")
    private String foodType; // VEG, NON_VEG, BOTH, NONE

    @Column(name = "gate_closing_time")
    private java.time.LocalTime gateClosingTime;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "security_deposit_paise")
    private Long securityDepositPaise;

    @Column(name = "deposit_type", length = 20)
    @Builder.Default
    private String depositType = "REFUNDABLE";

    @Column(name = "deposit_terms", columnDefinition = "TEXT")
    private String depositTerms;

    @Column(name = "maintenance_charge_paise")
    private Long maintenanceChargePaise;

    @Column(name = "min_lease_months")
    private Integer minLeaseMonths;

    // ── Rental-specific fields (NoBroker/99acres parity) ──
    @Column(name = "apartment_name", length = 200)
    private String apartmentName; // Society/building name

    @Column(name = "apartment_type", length = 30)
    private String apartmentType; // GATED_COMMUNITY, STANDALONE, VILLA, BUILDER_FLOOR, PENTHOUSE

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "property_age", length = 20)
    private String propertyAge; // NEW, 1_TO_3, 3_TO_5, 5_TO_10, 10_PLUS

    @Column(name = "facing", length = 20)
    private String facing; // NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST

    @Column(name = "built_up_area_sqft")
    private Integer builtUpAreaSqft;

    @Column(name = "rental_type", length = 15)
    private String rentalType; // RENT, LEASE, RENT_OR_LEASE

    @Column(name = "rent_negotiable")
    @Builder.Default
    private Boolean rentNegotiable = false;

    @Column(name = "maintenance_included")
    @Builder.Default
    private Boolean maintenanceIncluded = false;

    @Column(name = "available_from")
    private java.time.LocalDate availableFrom;

    @Column(name = "preferred_tenants", length = 100)
    private String preferredTenants; // ANYONE, FAMILY, BACHELOR_MALE, BACHELOR_FEMALE, COMPANY (comma-separated)

    @Column(name = "water_supply", length = 30)
    private String waterSupply; // MUNICIPAL, BOREWELL, BOTH, TANKER

    @Column(name = "gated_security")
    @Builder.Default
    private Boolean gatedSecurity = false;

    @Column(name = "non_veg_allowed")
    @Builder.Default
    private Boolean nonVegAllowed = true;

    @Column(name = "property_condition", length = 20)
    private String propertyCondition; // VACANT, OCCUPIED, UNDER_RENOVATION

    @Column(name = "show_property_by", length = 30)
    private String showPropertyBy; // OWNER, AGENT, CARETAKER, KEY_WITH_NEIGHBOR

    @Column(name = "direction_tips", columnDefinition = "TEXT")
    private String directionTips;

    @Column(name = "visit_availability", length = 50)
    private String visitAvailability; // EVERYDAY, WEEKDAY, WEEKEND, CUSTOM

    @Column(name = "visit_time_from", length = 10)
    private String visitTimeFrom;

    @Column(name = "visit_time_until", length = 10)
    private String visitTimeUntil;

    @Column(name = "secondary_phone", length = 15)
    private String secondaryPhone;

    @Column(name = "multiple_units")
    @Builder.Default
    private Boolean multipleUnits = false;

    // Insurance
    @Column(name = "insurance_enabled")
    @Builder.Default
    private Boolean insuranceEnabled = false;

    @Column(name = "insurance_amount_paise")
    private Long insuranceAmountPaise; // per booking

    @Column(name = "insurance_type", length = 30)
    private String insuranceType; // BASIC, PREMIUM, DAMAGE_PROTECTION

    // Rental rules (monthly)
    @Column(name = "rent_payment_day")
    @Builder.Default
    private Integer rentPaymentDay = 1;

    @Column(name = "visitor_policy", length = 20)
    private String visitorPolicy; // ALLOWED, RESTRICTED, NO_OVERNIGHT, NOT_ALLOWED

    @Column(name = "quiet_hours_from", length = 10)
    private String quietHoursFrom;

    @Column(name = "quiet_hours_until", length = 10)
    private String quietHoursUntil;

    @Column(name = "smoking_allowed")
    @Builder.Default
    private Boolean smokingAllowed = false;

    @Column(name = "grace_period_days")
    @Builder.Default
    private Integer gracePeriodDays = 5;

    @Column(name = "late_penalty_bps")
    @Builder.Default
    private Integer latePenaltyBps = 200;

    // Hotel specific
    @Column(name = "hotel_chain")
    private String hotelChain;

    @Column(name = "front_desk_24h")
    @Builder.Default
    private Boolean frontDesk24h = false;

    @Column(name = "checkout_time")
    private java.time.LocalTime checkoutTime;

    @Column(name = "checkin_time")
    private java.time.LocalTime checkinTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ── Non-refundable rate & Payment mode ──
    @Column(name = "non_refundable_discount_percent")
    private Integer nonRefundableDiscountPercent; // e.g. 10 = 10% off for non-refundable

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20)
    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.PREPAID;

    @Column(name = "pay_at_property_enabled")
    @Builder.Default
    private Boolean payAtPropertyEnabled = false;

    @Column(name = "partial_prepaid_percent")
    private Integer partialPrepaidPercent; // e.g. 30 = 30% now, 70% at property

    // ── Hotel enhancements ──
    @Column(name = "couple_friendly")
    @Builder.Default
    private Boolean coupleFriendly = false;

    @Column(name = "property_highlights", columnDefinition = "TEXT")
    private String propertyHighlights; // comma-separated

    @Column(name = "early_bird_discount_percent")
    @Builder.Default
    private Integer earlyBirdDiscountPercent = 0;

    @Column(name = "early_bird_days_before")
    @Builder.Default
    private Integer earlyBirdDaysBefore = 30;

    @Column(name = "zero_payment_booking")
    @Builder.Default
    private Boolean zeroPaymentBooking = false;

    @Column(name = "location_highlight", length = 200)
    private String locationHighlight;

    // ── Discovery categories (India vibe-based) ──
    @Column(name = "discovery_categories", length = 500)
    private String discoveryCategories; // comma-separated: "HILL_STATIONS,WEEKEND_GETAWAYS"

    // ── Archive / Suspension fields ──
    @Enumerated(EnumType.STRING)
    @Column(name = "archive_reason", length = 30)
    private ArchiveReason archiveReason;

    @Column(name = "archive_note", columnDefinition = "TEXT")
    private String archiveNote;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;
}
