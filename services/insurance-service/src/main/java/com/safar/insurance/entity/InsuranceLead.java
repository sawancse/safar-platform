package com.safar.insurance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Talk-to-advisor / assisted-sales lead (PolicyBazaar tele-advisor model). */
@Entity
@Table(name = "insurance_leads", schema = "insurance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 40)
    private String product;

    @Column(length = 30)
    private String coverageType;

    @Column(length = 120)
    private String city;

    @Column(length = 60)
    private String preferredTime;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "NEW";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String source = "WEB_ADVISOR";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
