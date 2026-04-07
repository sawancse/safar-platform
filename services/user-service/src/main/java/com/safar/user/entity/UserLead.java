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

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
