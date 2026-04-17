package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "advocates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advocate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(name = "bar_council_number", length = 100)
    private String barCouncilId;

    @Column(length = 200)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    private Integer experienceYears;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] specializations;

    private String profilePhotoUrl;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Builder.Default
    private Integer totalCases = 0;

    @Column(name = "cases_completed")
    @Builder.Default
    private Integer completedCases = 0;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean active = true;

    private Long consultationFeePaise;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // Onboarding fields
    @Column(name = "user_id")
    private UUID userId; // linked to auth-service user account

    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_by")
    private UUID verifiedBy; // admin who approved

    private OffsetDateTime verifiedAt;

    // Documents uploaded during registration
    @Column(name = "id_proof_url", columnDefinition = "TEXT")
    private String idProofUrl;

    @Column(name = "license_url", columnDefinition = "TEXT")
    private String licenseUrl;

    @Column(name = "certificate_urls", columnDefinition = "TEXT")
    private String certificateUrls; // JSON array

    @Column(name = "languages", columnDefinition = "TEXT")
    private String languages; // JSON array e.g. ["English","Hindi","Telugu"]

    @Column(name = "available_days", length = 50)
    private String availableDays; // e.g. "MON-FRI" or "MON,WED,FRI"

    @Column(name = "available_hours", length = 30)
    private String availableHours; // e.g. "10:00-18:00"

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
