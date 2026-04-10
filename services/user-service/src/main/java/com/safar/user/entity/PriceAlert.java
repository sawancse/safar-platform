package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_alerts", schema = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceAlert {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String email;
    private UUID userId;
    @Column(nullable = false) private UUID listingId;
    private String listingTitle;
    private String listingCity;
    @Column(nullable = false) private Long thresholdPaise;
    private Long currentPricePaise;
    @Builder.Default private Boolean active = true;
    @Builder.Default private Integer triggeredCount = 0;
    private OffsetDateTime lastTriggeredAt;
    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp private OffsetDateTime updatedAt;
}
