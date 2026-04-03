package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "locality_price_trends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalityPriceTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String locality;

    @Column(nullable = false)
    private LocalDate month;

    private Long avgPricePerSqftPaise;

    private Long medianPricePerSqftPaise;

    @Builder.Default
    private Integer totalListings = 0;

    @Builder.Default
    private Integer totalSold = 0;

    @Column(length = 30)
    private String propertyType;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
