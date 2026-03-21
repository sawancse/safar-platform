package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_installations", schema = "listings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"app_id", "host_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "scopes_granted", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String scopesGranted = "";

    @Column(name = "access_token", length = 256)
    private String accessToken;

    @Column(name = "refresh_token", length = 256)
    private String refreshToken;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "installed_at", updatable = false)
    private OffsetDateTime installedAt;
}
