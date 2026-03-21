package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cleaner_profiles", schema = "bookings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CleanerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private String cities = "";

    @Column(nullable = false)
    private Long ratePerHourPaise;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(nullable = false)
    @Builder.Default
    private Integer jobCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
