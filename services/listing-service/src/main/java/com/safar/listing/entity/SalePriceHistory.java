package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sale_price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID salePropertyId;

    @Column(nullable = false)
    private Long pricePaise;

    private Long pricePerSqftPaise;

    @Builder.Default
    private OffsetDateTime changedAt = OffsetDateTime.now();
}
