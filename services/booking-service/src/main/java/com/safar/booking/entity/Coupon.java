package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons", schema = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String scope = "PLATFORM";          // PLATFORM | HOST

    @Column(name = "owner_id")
    private UUID ownerId;                         // host user id for HOST scope

    @Column(name = "listing_id")
    private UUID listingId;                       // optional: restrict to one listing

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;                 // PERCENT | FLAT

    @Column(name = "percent_off")
    private Integer percentOff;

    @Column(name = "max_discount_paise")
    private Long maxDiscountPaise;

    @Column(name = "flat_off_paise")
    private Long flatOffPaise;

    @Column(name = "min_booking_paise", nullable = false)
    @Builder.Default
    private Long minBookingPaise = 0L;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
