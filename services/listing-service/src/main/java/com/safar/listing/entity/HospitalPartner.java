package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_partners", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(nullable = false)
    @Builder.Default
    private String specialties = "";

    @Column(nullable = false)
    @Builder.Default
    private String accreditations = "";

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "airport_distance_km", precision = 5, scale = 1)
    private BigDecimal airportDistanceKm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
