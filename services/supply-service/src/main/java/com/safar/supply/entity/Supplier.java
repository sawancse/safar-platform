package com.safar.supply.entity;

import com.safar.supply.entity.enums.IntegrationType;
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
@Table(name = "suppliers", schema = "supply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @Column(columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] categories;

    @Column(name = "service_cities", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] serviceCities;

    @Column(name = "lead_time_days")
    @Builder.Default
    private Integer leadTimeDays = 2;

    @Column(name = "payment_terms", length = 40)
    @Builder.Default
    private String paymentTerms = "NET_15";

    @Column(name = "credit_limit_paise")
    @Builder.Default
    private Long creditLimitPaise = 0L;

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

    @Column(name = "pos_completed")
    @Builder.Default
    private Integer posCompleted = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Integration (Phase 2) ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 30)
    @Builder.Default
    private IntegrationType integrationType = IntegrationType.MANUAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "integration_config", columnDefinition = "jsonb")
    private String integrationConfig;

    @Column(name = "catalog_synced_at")
    private OffsetDateTime catalogSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
