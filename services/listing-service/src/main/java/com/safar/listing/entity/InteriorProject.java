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
@Table(name = "interior_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID designerId;

    private UUID listingId;

    private UUID salePropertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteriorProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InteriorProjectStatus status = InteriorProjectStatus.CONSULTATION_BOOKED;

    @Enumerated(EnumType.STRING)
    private DesignStyle designStyle;

    @Column(length = 500)
    private String propertyAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    private Integer areaSqft;

    private Long budgetMinPaise;

    private Long budgetMaxPaise;

    private Long quotedAmountPaise;

    private Long approvedAmountPaise;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] currentPhotos;

    private LocalDate expectedStartDate;

    private LocalDate expectedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Builder.Default
    private Boolean paid = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
