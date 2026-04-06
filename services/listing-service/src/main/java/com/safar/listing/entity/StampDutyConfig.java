package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stamp_duty_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampDutyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementType agreementType;

    @Column(name = "duty_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal stampDutyPercent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal registrationPercent;

    private Long minimumStampPaise;

    private Long maximumStampPaise;

    @Column(precision = 5, scale = 2)
    private BigDecimal surchargePercent;

    @Column(precision = 5, scale = 2)
    private BigDecimal cessPercent;

    @Builder.Default
    private Boolean active = true;

    private LocalDate effectiveFrom;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
