package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String currentMedications;

    @Column(columnDefinition = "TEXT")
    private String pastSurgeries;

    @Column(columnDefinition = "TEXT")
    private String chronicConditions;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'English'")
    private String preferredLanguage;

    @Column(columnDefinition = "TEXT")
    private String dietaryRestrictions;

    @Column(columnDefinition = "TEXT")
    private String mobilityNeeds;

    private String insuranceProvider;
    private String insurancePolicyNumber;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
