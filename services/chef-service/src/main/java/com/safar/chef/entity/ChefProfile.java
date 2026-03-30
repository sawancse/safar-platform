package com.safar.chef.entity;

import com.safar.chef.entity.enums.ChefType;
import com.safar.chef.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_profiles", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "chef_type")
    private ChefType chefType;

    @Column(name = "experience_years")
    private Integer experienceYears;

    private String city;

    private String state;

    private String pincode;

    @Column(columnDefinition = "TEXT")
    private String cuisines;

    @Column(columnDefinition = "TEXT")
    private String specialties;

    @Column(columnDefinition = "TEXT")
    private String localities;

    @Column(name = "daily_rate_paise")
    private Long dailyRatePaise;

    @Column(name = "monthly_rate_paise")
    private Long monthlyRatePaise;

    @Column(name = "event_min_plate_paise")
    private Long eventMinPlatePaise;

    @Column(name = "min_guests")
    @Builder.Default
    private Integer minGuests = 1;

    @Column(name = "max_guests")
    @Builder.Default
    private Integer maxGuests = 100;

    @Column(name = "event_min_pax")
    private Integer eventMinPax;

    @Column(name = "event_max_pax")
    private Integer eventMaxPax;

    @Column(columnDefinition = "TEXT")
    private String languages;

    @Builder.Default
    private Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "id_proof_type")
    private String idProofType;

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Column(name = "food_safety_certificate")
    @Builder.Default
    private Boolean foodSafetyCertificate = false;

    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "total_bookings")
    @Builder.Default
    private Integer totalBookings = 0;

    @Column(name = "completion_rate")
    @Builder.Default
    private Double completionRate = 100.0;

    @Builder.Default
    private Boolean available = true;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
