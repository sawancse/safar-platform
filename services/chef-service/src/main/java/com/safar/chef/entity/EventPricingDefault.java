package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_pricing_defaults", schema = "chefs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventPricingDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "item_key", nullable = false, unique = true, length = 50)
    private String itemKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 255)
    private String description;

    @Column(length = 10)
    private String icon;

    @Column(name = "default_price_paise", nullable = false)
    private Long defaultPricePaise;

    @Column(name = "price_type", nullable = false, length = 20)
    private String priceType;

    @Column(name = "min_price_paise")
    private Long minPricePaise;

    @Column(name = "max_price_paise")
    private Long maxPricePaise;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
