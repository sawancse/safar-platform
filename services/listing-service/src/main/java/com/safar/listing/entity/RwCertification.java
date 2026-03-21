package com.safar.listing.entity;

import com.safar.listing.entity.enums.RwCertStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rw_certifications", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RwCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RwCertStatus status = RwCertStatus.PENDING;

    @Column(name = "wifi_speed_mbps")
    private Integer wifiSpeedMbps;

    @Column(name = "has_dedicated_desk")
    @Builder.Default
    private Boolean hasDedicatedDesk = false;

    @Column(name = "has_power_backup")
    @Builder.Default
    private Boolean hasPowerBackup = false;

    @Column(name = "quiet_hours_from")
    private LocalTime quietHoursFrom;

    @Column(name = "quiet_hours_to")
    private LocalTime quietHoursTo;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "certified_at")
    private OffsetDateTime certifiedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;
}
