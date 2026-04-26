package com.safar.insurance.entity;

import com.safar.insurance.entity.enums.CoverageType;
import com.safar.insurance.entity.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "insurance_policies", schema = "insurance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 30)
    private String policyRef;

    @Column(nullable = false, length = 20)
    private String provider;          // ACKO / ICICI_LOMBARD / RELIANCE_GENERAL / HDFC_ERGO

    @Column(length = 100)
    private String externalPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoverageType coverageType;

    @Column(length = 5)
    private String tripOriginCode;

    @Column(length = 5)
    private String tripDestinationCode;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String tripOriginCountry = "IN";

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String tripDestinationCountry = "IN";

    @Column(nullable = false)
    private LocalDate tripStartDate;

    @Column(nullable = false)
    private LocalDate tripEndDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer insuredCount = 1;

    @Column(columnDefinition = "TEXT")
    private String insuredJson;

    @Column(length = 200)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(nullable = false)
    private Long premiumPaise;

    private Long sumInsuredPaise;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    private Instant issuedAt;
    private Instant cancelledAt;

    @Column(length = 500)
    private String cancellationReason;

    private Long refundAmountPaise;

    @Column(length = 500)
    private String certificateUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
