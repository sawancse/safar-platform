package com.safar.listing.entity;

import com.safar.listing.entity.enums.FinancingType;
import com.safar.listing.entity.enums.InquiryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID salePropertyId;

    private UUID builderProjectId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 100)
    private String buyerName;

    @Column(length = 15)
    private String buyerPhone;

    @Column(length = 200)
    private String buyerEmail;

    private LocalDate preferredVisitDate;

    @Column(length = 20)
    private String preferredVisitTime;

    @Enumerated(EnumType.STRING)
    private FinancingType financingType;

    private Long budgetMinPaise;

    private Long budgetMaxPaise;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
