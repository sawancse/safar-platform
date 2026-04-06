package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String bankName;

    @Column(length = 200)
    private String logoUrl;

    @Column(name = "min_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRateMin;

    @Column(name = "max_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRateMax;

    @Column(nullable = false)
    private Integer maxTenureMonths;

    private Long maxLoanAmountPaise;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal processingFeePercent;

    private Long processingFeeMinPaise;

    private Long processingFeeMaxPaise;

    @Builder.Default
    private Boolean preApprovalAvailable = false;

    @Builder.Default
    private Boolean balanceTransferAvailable = false;

    @Column(length = 500)
    private String specialOffers;

    @Column(length = 200)
    private String contactName;

    @Column(length = 200)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
