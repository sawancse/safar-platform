package com.safar.services.entity;

import com.safar.services.entity.enums.CuisineType;
import com.safar.services.entity.enums.MealType;
import com.safar.services.entity.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_menus", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cuisine_type")
    private CuisineType cuisineType;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType mealType;

    @Column(name = "price_per_plate_paise")
    private Long pricePerPlatePaise;

    @Column(name = "min_guests")
    @Builder.Default
    private Integer minGuests = 1;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "is_veg")
    @Builder.Default
    private Boolean isVeg = false;

    @Column(name = "is_vegan")
    @Builder.Default
    private Boolean isVegan = false;

    @Column(name = "is_jain")
    @Builder.Default
    private Boolean isJain = false;

    @Column(name = "photo_url")
    private String photoUrl;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
