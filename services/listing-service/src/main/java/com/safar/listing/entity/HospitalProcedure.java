package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_procedures", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HospitalProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "procedure_name", nullable = false)
    private String procedureName;

    @Column(nullable = false)
    private String specialty;

    @Column(name = "est_cost_min_paise", nullable = false)
    private Long estCostMinPaise;

    @Column(name = "est_cost_max_paise", nullable = false)
    private Long estCostMaxPaise;

    @Column(name = "hospital_days")
    @Builder.Default
    private Integer hospitalDays = 3;

    @Column(name = "recovery_days")
    @Builder.Default
    private Integer recoveryDays = 7;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
