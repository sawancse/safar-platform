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
@Table(name = "interior_designers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorDesigner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String companyName;

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] portfolioUrls;

    private String profilePhotoUrl;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Builder.Default
    private Integer totalProjects = 0;

    @Column(name = "projects_completed")
    @Builder.Default
    private Integer completedProjects = 0;

    private Long consultationFeePaise;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // Onboarding fields
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private String verificationStatus = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    private OffsetDateTime verifiedAt;

    @Column(name = "id_proof_url", columnDefinition = "TEXT")
    private String idProofUrl;

    @Column(name = "license_url", columnDefinition = "TEXT")
    private String licenseUrl;

    @Column(name = "certificate_urls", columnDefinition = "TEXT")
    private String certificateUrls;

    @Column(name = "iiid_membership", length = 50)
    private String iiidMembership; // Institute of Indian Interior Designers

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "service_areas", columnDefinition = "TEXT")
    private String serviceAreas; // JSON array of cities/localities served

    @Column(name = "min_budget_paise")
    private Long minBudgetPaise; // minimum project budget they accept

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
