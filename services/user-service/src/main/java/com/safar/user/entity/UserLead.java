package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_leads", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    private String name;
    private String phone;
    private String city;

    @Column(nullable = false)
    @Builder.Default
    private String source = "WEBSITE_POPUP";

    private String utmSource;
    private String utmMedium;
    private String utmCampaign;

    @Builder.Default
    private Boolean subscribed = true;

    @Builder.Default
    private Boolean converted = false;

    private UUID convertedUserId;

    // Scoring
    @Builder.Default
    private Integer leadScore = 0;
    @Builder.Default
    private Integer intentScore = 0;
    @Builder.Default
    private Integer behavioralScore = 0;
    @Builder.Default
    private Integer demographicScore = 0;
    @Builder.Default
    private Integer recencyScore = 0;

    @Column(length = 30)
    @Builder.Default
    private String segment = "NEW";

    @Builder.Default
    private Boolean whatsappOptin = false;

    private OffsetDateTime lastActiveAt;

    @Builder.Default
    private Integer pagesViewed = 0;
    @Builder.Default
    private Integer searchesPerformed = 0;
    @Builder.Default
    private Integer listingsViewed = 0;
    @Builder.Default
    private Integer wishlistCount = 0;
    @Builder.Default
    private Boolean checkoutAttempted = false;

    @Column(length = 500)
    private String lastSearchQuery;
    @Column(length = 100)
    private String lastSearchCity;

    // Nurturing
    @Column(length = 30)
    @Builder.Default
    private String nurtureStage = "NONE";
    @Builder.Default
    @Column(name = "nurture_day0_sent")
    private Boolean nurtureDay0Sent = false;
    @Builder.Default
    @Column(name = "nurture_day3_sent")
    private Boolean nurtureDay3Sent = false;
    @Builder.Default
    @Column(name = "nurture_day7_sent")
    private Boolean nurtureDay7Sent = false;

    private OffsetDateTime convertedAt;

    @Column(length = 20)
    @Builder.Default
    private String leadType = "GUEST";

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
