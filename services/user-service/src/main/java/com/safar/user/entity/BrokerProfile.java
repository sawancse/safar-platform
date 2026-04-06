package com.safar.user.entity;

import com.safar.user.entity.enums.BrokerSpecialization;
import com.safar.user.entity.enums.BrokerSubscriptionTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "broker_profiles", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BrokerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "rera_agent_id", length = 50)
    private String reraAgentId;

    @Column(name = "rera_verified")
    @Builder.Default
    private Boolean reraVerified = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "operating_cities", columnDefinition = "text[]")
    private List<String> operatingCities;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private BrokerSpecialization specialization = BrokerSpecialization.RESIDENTIAL;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(name = "total_deals_closed")
    @Builder.Default
    private Integer totalDealsCount = 0;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 300)
    private String website;

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "office_city", length = 100)
    private String officeCity;

    @Column(name = "office_state", length = 100)
    private String officeState;

    @Column(name = "office_pincode", length = 10)
    private String officePincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", length = 20)
    @Builder.Default
    private BrokerSubscriptionTier subscriptionTier = BrokerSubscriptionTier.FREE;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
