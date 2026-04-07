package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dish_ingredients", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String category = "GROCERY";

    @Column(name = "is_optional")
    @Builder.Default
    private Boolean isOptional = false;

    @Column(length = 300)
    private String notes;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
