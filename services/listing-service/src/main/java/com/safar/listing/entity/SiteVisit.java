package com.safar.listing.entity;

import com.safar.listing.entity.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "site_visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID inquiryId;

    @Column(nullable = false)
    private UUID salePropertyId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private OffsetDateTime scheduledAt;

    @Builder.Default
    private Integer durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VisitStatus status = VisitStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String buyerFeedback;

    @Column(columnDefinition = "TEXT")
    private String sellerFeedback;

    private Integer rating;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
