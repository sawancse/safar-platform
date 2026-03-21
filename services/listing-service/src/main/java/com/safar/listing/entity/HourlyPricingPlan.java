package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "hourly_pricing_plans", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyPricingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "slot_hours", nullable = false)
    private Integer slotHours; // 3, 6, or 9

    @Column(name = "price_paise", nullable = false)
    private Long pricePaise;

    @Column(name = "available_from")
    @Builder.Default
    private LocalTime availableFrom = LocalTime.of(6, 0);

    @Column(name = "available_until")
    @Builder.Default
    private LocalTime availableUntil = LocalTime.of(22, 0);
}
