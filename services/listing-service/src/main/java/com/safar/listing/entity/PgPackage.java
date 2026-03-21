package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pg_packages", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price_paise", nullable = false)
    private Long monthlyPricePaise;

    @Column(name = "includes_meals")
    @Builder.Default
    private Boolean includesMeals = false;

    @Column(name = "includes_laundry")
    @Builder.Default
    private Boolean includesLaundry = false;

    @Column(name = "includes_wifi")
    @Builder.Default
    private Boolean includesWifi = false;

    @Column(name = "includes_housekeeping")
    @Builder.Default
    private Boolean includesHousekeeping = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
