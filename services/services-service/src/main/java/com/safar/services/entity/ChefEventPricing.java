package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_event_pricing", schema = "chefs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chef_id", "item_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChefEventPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "item_key", nullable = false, length = 50)
    private String itemKey;

    @Column(name = "custom_price_paise", nullable = false)
    private Long customPricePaise;

    @Column(nullable = false)
    private Boolean available = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
