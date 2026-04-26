package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * BD-agent-issued invite for a prospective vendor (Pattern E).
 * Token in invite URL → wizard pre-fills phone + OTP-skip → vendor onboards.
 *
 * Funnel: sent_at → opened_at → onboarding_started_at → submitted_at →
 * completed_at. Each transition lets us measure where vendors drop off.
 */
@Entity
@Table(name = "vendor_invites", schema = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invite_token", nullable = false, length = 64, unique = true)
    private String inviteToken;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Funnel timestamps
    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @Column(name = "opened_at")
    private OffsetDateTime openedAt;

    @Column(name = "onboarding_started_at")
    private OffsetDateTime onboardingStartedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "sent_via", nullable = false, length = 20)
    private String sentVia = "MANUAL";

    @Column(name = "sent_by")
    private UUID sentBy;

    @Column(name = "service_listing_id")
    private UUID serviceListingId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
