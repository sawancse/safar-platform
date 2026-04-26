package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "menu_items", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "is_veg")
    @Builder.Default
    private Boolean isVeg = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
