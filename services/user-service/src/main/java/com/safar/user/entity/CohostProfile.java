package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cohost_profiles", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CohostProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID hostId;

    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private String servicesOffered = "";

    @Column(nullable = false)
    @Builder.Default
    private String cities = "";

    @Column(nullable = false)
    @Builder.Default
    private Integer minFeePct = 5;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxFeePct = 15;

    @Builder.Default
    private Integer maxListings = 5;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentListings = 0;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
