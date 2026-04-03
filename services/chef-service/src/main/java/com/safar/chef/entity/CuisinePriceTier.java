package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cuisine_price_tiers", schema = "chefs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"chef_id", "cuisine_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuisinePriceTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "cuisine_type", nullable = false, length = 30)
    private String cuisineType;

    @Column(name = "price_per_plate_paise", nullable = false)
    private Long pricePerPlatePaise;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
