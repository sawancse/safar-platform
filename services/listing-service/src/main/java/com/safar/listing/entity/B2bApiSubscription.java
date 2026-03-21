package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "b2b_api_subscriptions", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B2bApiSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 255)
    private String contactEmail;

    @Column(nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String plan = "STARTER";

    @Column(nullable = false)
    @Builder.Default
    private Long monthlyCalls = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long callLimit = 10000L;

    @Column(nullable = false)
    private Long priceMonthlyPaise;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
