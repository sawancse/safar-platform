package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "property_id")
    private UUID salePropertyId;

    private UUID listingId;

    private UUID advocateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LegalPackageType packageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LegalCaseStatus status = LegalCaseStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.GREEN;

    @Column(length = 200)
    private String propertyAddress;

    @Column(name = "property_city", length = 100)
    private String city;

    @Column(name = "property_state", length = 100)
    private String state;

    @Column(columnDefinition = "TEXT")
    private String reportSummary;

    @Column(name = "report_pdf_url")
    private String reportUrl;

    @Column(name = "service_fees_paise")
    private Long feePaise;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Builder.Default
    private Boolean paid = false;

    private OffsetDateTime assignedAt;

    private OffsetDateTime reportReadyAt;

    private OffsetDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
