package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "room_type_availability", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "available_count", nullable = false)
    private Integer availableCount;

    @Column(name = "price_override_paise")
    private Long priceOverridePaise;

    @Column(name = "min_stay_nights")
    @Builder.Default
    private Integer minStayNights = 1;

    @Version
    private Long version;
}
