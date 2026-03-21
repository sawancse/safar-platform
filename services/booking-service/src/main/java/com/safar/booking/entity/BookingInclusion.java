package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_inclusions", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "inclusion_id", nullable = false)
    private UUID inclusionId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "inclusion_mode", nullable = false, length = 20)
    private String inclusionMode;

    @Column(name = "charge_paise")
    @Builder.Default
    private Long chargePaise = 0L;

    @Column(name = "charge_type", length = 20)
    @Builder.Default
    private String chargeType = "PER_STAY";

    @Column(name = "discount_percent")
    @Builder.Default
    private Integer discountPercent = 0;

    @Column(length = 500)
    private String terms;

    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "total_paise")
    @Builder.Default
    private Long totalPaise = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
