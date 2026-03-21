package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "location_suggestions", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, length = 30)
    private String type; // CITY, LOCALITY, LANDMARK, IT_PARK, COLLEGE, HOSPITAL, TRANSIT

    @Column(nullable = false)
    private String city;

    @Column
    private String state;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "popularity_score")
    @Builder.Default
    private Integer popularityScore = 0;

    @Column(name = "default_radius_km")
    @Builder.Default
    private Double defaultRadiusKm = 5.0;
}
