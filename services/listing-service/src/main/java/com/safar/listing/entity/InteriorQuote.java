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
@Table(name = "interior_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID interiorProjectId;

    @Column(length = 50)
    private String quoteNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.DRAFT;

    private Long materialCostPaise;

    @Column(name = "labor_cost_paise")
    private Long labourCostPaise;

    @Column(name = "overhead_paise")
    private Long overheadCostPaise;

    private Long discountPaise;

    private Long taxPaise;

    @Column(name = "total_paise")
    private Long totalAmountPaise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String lineItemsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String termsJson;

    private LocalDate validUntil;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private OffsetDateTime sentAt;

    private OffsetDateTime approvedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
