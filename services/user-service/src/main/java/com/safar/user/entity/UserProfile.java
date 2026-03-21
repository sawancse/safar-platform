package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private String name = "";

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String phone;

    @Column(length = 20)
    @Builder.Default
    private String role = "GUEST";

    @Column(nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(length = 60)
    @Builder.Default
    private String nationality = "India";

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "passport_name", length = 200)
    private String passportName;

    @Column(name = "passport_number", length = 30)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "verification_level", length = 20)
    @Builder.Default
    private String verificationLevel = "UNVERIFIED";

    @Column(name = "trust_score")
    @Builder.Default
    private Integer trustScore = 0;

    @Column(name = "host_type", length = 30)
    @Builder.Default
    private String hostType = "INDIVIDUAL";

    @Column(name = "resident_status", length = 10)
    @Builder.Default
    private String residentStatus = "RESIDENT";

    @Column(name = "monthly_payout_cap_paise")
    @Builder.Default
    private Long monthlyPayoutCapPaise = 1000000L;

    @Column(name = "fraud_risk_score")
    @Builder.Default
    private Integer fraudRiskScore = 0;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 200)
    private String languages; // comma-separated: "English,Hindi,Tamil"

    @Column(name = "response_rate")
    @Builder.Default
    private Integer responseRate = 0; // 0-100 percentage

    @Column(name = "avg_response_minutes")
    private Integer avgResponseMinutes;

    @Column(name = "total_host_reviews")
    @Builder.Default
    private Integer totalHostReviews = 0;

    @Column(name = "last_active_at")
    private OffsetDateTime lastActiveAt;

    @Column(name = "profile_completion")
    @Builder.Default
    private Integer profileCompletion = 0; // 0-100

    @Column(name = "selfie_verified")
    @Builder.Default
    private Boolean selfieVerified = false;

    @Column(name = "digilocker_verified")
    @Builder.Default
    private Boolean digilockerVerified = false;

    @Column(name = "verification_expires_at")
    private OffsetDateTime verificationExpiresAt;

    // ── Safar Star Host (earned badge) ──
    @Column(name = "star_host")
    @Builder.Default
    private Boolean starHost = false;

    @Column(name = "star_host_since")
    private OffsetDateTime starHostSince;

    @Column(name = "avg_host_rating")
    private Double avgHostRating; // synced from review-service

    @Column(name = "cancellation_rate_percent")
    @Builder.Default
    private Double cancellationRatePercent = 0.0;

    @Column(name = "total_completed_stays")
    @Builder.Default
    private Integer totalCompletedStays = 0;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
