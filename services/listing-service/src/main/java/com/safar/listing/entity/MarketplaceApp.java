package com.safar.listing.entity;

import com.safar.listing.entity.enums.AppStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_apps", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "developer_id", nullable = false)
    private UUID developerId;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 256)
    private String clientSecret;

    @Column(name = "redirect_uris", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String redirectUris = "";

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String scopes = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppStatus status = AppStatus.PENDING;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 64)
    private String webhookSecret;

    @Column(name = "rate_limit_rpm", nullable = false)
    @Builder.Default
    private Integer rateLimitRpm = 60;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
