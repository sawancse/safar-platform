package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "property_id")
    private UUID salePropertyId;

    @Column(name = "bank_id")
    private UUID partnerBankId;

    @Column(length = 50)
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanApplicationStatus status = LoanApplicationStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Column(name = "loan_amount_paise")
    private Long requestedAmountPaise;

    @Column(name = "tenure_months")
    private Integer requestedTenureMonths;

    private Long sanctionedAmountPaise;

    @Column(precision = 5, scale = 2)
    private BigDecimal sanctionedInterestRate;

    private Integer sanctionedTenureMonths;

    private Long estimatedEmiPaise;

    private Long propertyValuePaise;

    @Column(length = 200)
    private String applicantName;

    @Column(length = 200)
    private String applicantEmail;

    @Column(length = 20)
    private String applicantPhone;

    @Column(length = 12)
    private String panNumber;

    private Long monthlyIncomePaise;

    @Column(length = 200)
    private String employerName;

    private Integer workExperienceYears;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String bankReferenceId;

    private OffsetDateTime sanctionedAt;

    private OffsetDateTime disbursedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
