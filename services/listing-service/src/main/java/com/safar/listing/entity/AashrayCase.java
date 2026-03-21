package com.safar.listing.entity;

import com.safar.listing.entity.enums.CasePriority;
import com.safar.listing.entity.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "aashray_cases", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AashrayCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_number", nullable = false, unique = true, length = 20)
    private String caseNumber;

    @Column(name = "seeker_name", nullable = false)
    private String seekerName;

    @Column(name = "seeker_phone")
    private String seekerPhone;

    @Column(name = "seeker_email")
    private String seekerEmail;

    @Column(name = "family_size", nullable = false)
    @Builder.Default
    private int familySize = 1;

    @Column(nullable = false)
    @Builder.Default
    private int children = 0;

    @Column(nullable = false)
    @Builder.Default
    private int elderly = 0;

    @Column(name = "current_city", length = 100)
    private String currentCity;

    @Column(name = "preferred_city", nullable = false, length = 100)
    private String preferredCity;

    @Column(name = "preferred_locality")
    private String preferredLocality;

    @Column(name = "budget_max_paise", nullable = false)
    @Builder.Default
    private long budgetMaxPaise = 0;

    @Column(name = "languages_spoken", length = 500)
    private String languagesSpoken;

    @Column(name = "special_needs", columnDefinition = "TEXT")
    private String specialNeeds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(name = "referral_source")
    private String referralSource;

    @Column(name = "matched_listing_id")
    private UUID matchedListingId;

    @Column(name = "assigned_ngo_id")
    private UUID assignedNgoId;

    @Column(name = "need_by_date")
    private LocalDate needByDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
