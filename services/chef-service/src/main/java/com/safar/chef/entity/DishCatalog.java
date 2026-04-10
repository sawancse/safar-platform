package com.safar.chef.entity;

import com.safar.chef.entity.enums.DishCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dish_catalog", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DishCategory category;

    @Column(name = "price_paise", nullable = false)
    @Builder.Default
    private Long pricePaise = 0L;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "is_veg")
    @Builder.Default
    private Boolean isVeg = true;

    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

    @Column(name = "no_onion_garlic")
    @Builder.Default
    private Boolean noOnionGarlic = false;

    @Column(name = "is_fried")
    @Builder.Default
    private Boolean isFried = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
