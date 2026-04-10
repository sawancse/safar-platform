package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_dish_offerings", schema = "chefs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chef_id", "dish_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefDishOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "dish_id", nullable = false)
    private UUID dishId;

    @Column(name = "custom_price_paise")
    private Long customPricePaise;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
