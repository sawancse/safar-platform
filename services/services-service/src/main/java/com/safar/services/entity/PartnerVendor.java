package com.safar.services.entity;

import com.safar.services.entity.enums.VendorServiceType;
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
@Table(name = "partner_vendors", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private VendorServiceType serviceType;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 20)
    private String gst;

    @Column(length = 15)
    private String pan;

    @Column(name = "bank_account", length = 40)
    private String bankAccount;

    @Column(name = "bank_ifsc", length = 15)
    private String bankIfsc;

    @Column(name = "bank_holder", length = 120)
    private String bankHolder;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "service_cities", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] serviceCities;

    @Column(name = "service_radius_km")
    @Builder.Default
    private Integer serviceRadiusKm = 25;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_json", columnDefinition = "jsonb")
    private String portfolioJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pricing_override_json", columnDefinition = "jsonb")
    private String pricingOverrideJson;

    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "kyc_notes", columnDefinition = "TEXT")
    private String kycNotes;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "jobs_completed")
    @Builder.Default
    private Integer jobsCompleted = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
