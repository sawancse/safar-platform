package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agreement_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID listingId;

    @Column(name = "property_id")
    private UUID salePropertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementType agreementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false)
    @Builder.Default
    private AgreementPackage agreementPackage = AgreementPackage.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    private Long stampDutyPaise;

    private Long registrationFeePaise;

    @Column(name = "service_fees_paise")
    private Long serviceFeePaise;

    @Column(name = "total_paise")
    private Long totalFeePaise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "party_details_json", columnDefinition = "jsonb")
    private String termsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String clausesJson;

    @Column(name = "draft_pdf_url")
    private String documentUrl;

    @Column(name = "signed_pdf_url")
    private String signedDocumentUrl;

    @Column(name = "registered_pdf_url")
    private String registeredDocumentUrl;

    @Column(name = "e_stamp_id")
    private String stampCertificateNumber;

    private LocalDate agreementDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long monthlyRentPaise;

    private Long securityDepositPaise;

    private Long saleConsiderationPaise;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
