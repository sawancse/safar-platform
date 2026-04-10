package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "locality_alerts", schema = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LocalityAlert {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String email;
    private UUID userId;
    @Column(nullable = false) private String city;
    private String locality;
    private String listingType;
    private Long maxPricePaise;
    @Builder.Default private Boolean active = true;
    @Builder.Default private Integer triggeredCount = 0;
    private OffsetDateTime lastTriggeredAt;
    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp private OffsetDateTime updatedAt;
}
