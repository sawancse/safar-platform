package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
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
@Table(name = "loan_eligibilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    private Long monthlyIncomePaise;

    @Column(name = "current_emis_paise")
    private Long existingEmiPaise;

    @Builder.Default
    private Integer creditScore = 0;

    @Column(name = "max_eligible_amount_paise")
    private Long eligibleAmountPaise;

    @Column(precision = 5, scale = 2)
    private BigDecimal maxLtvPercent;

    @Column(precision = 5, scale = 2)
    private BigDecimal offeredInterestRate;

    private Integer offeredTenureMonths;

    @Column(name = "max_emi_paise")
    private Long estimatedEmiPaise;

    private UUID partnerBankId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String eligibilityDetailsJson;

    private OffsetDateTime calculatedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
