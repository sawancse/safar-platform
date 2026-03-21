package com.safar.listing.entity;

import com.safar.listing.entity.enums.GenderPolicy;
import com.safar.listing.entity.enums.ProfileStatus;
import com.safar.listing.entity.enums.SeekerType;
import com.safar.listing.entity.enums.SharingType;
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
@Table(name = "seeker_profiles", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeekerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "seeker_type", nullable = false, length = 30)
    @Builder.Default
    private SeekerType seekerType = SeekerType.PG_SEEKER;

    @Column(name = "preferred_city", nullable = false, length = 100)
    private String preferredCity;

    @Column(name = "preferred_locality")
    private String preferredLocality;

    @Column(name = "preferred_lat")
    private Double preferredLat;

    @Column(name = "preferred_lng")
    private Double preferredLng;

    @Column(name = "radius_km", nullable = false)
    @Builder.Default
    private int radiusKm = 5;

    @Column(name = "budget_min_paise", nullable = false)
    @Builder.Default
    private long budgetMinPaise = 0;

    @Column(name = "budget_max_paise", nullable = false)
    @Builder.Default
    private long budgetMaxPaise = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_sharing", length = 30)
    private SharingType preferredSharing;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", length = 20)
    private GenderPolicy genderPreference;

    @Column(name = "preferred_amenities", length = 500)
    private String preferredAmenities;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "vegetarian_only", nullable = false)
    @Builder.Default
    private boolean vegetarianOnly = false;

    @Column(name = "pet_owner", nullable = false)
    @Builder.Default
    private boolean petOwner = false;

    @Column(length = 50)
    private String occupation;

    @Column(name = "company_or_college")
    private String companyOrCollege;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Column(name = "match_count", nullable = false)
    @Builder.Default
    private int matchCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
