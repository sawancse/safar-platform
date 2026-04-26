package com.safar.services.service;

import com.safar.services.entity.VendorInvite;
import com.safar.services.repository.VendorInviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vendor invite token lifecycle (Pattern E — Zomato BD WhatsApp outreach).
 *
 * Flow:
 *  1. BD agent calls create(phone, type, ...) → token + deep-link URL returned.
 *     Agent pastes URL into WhatsApp (manual mode) or system templates it
 *     via MSG91 (V2).
 *  2. Vendor taps URL → wizard calls markOpened(token) → opened_at stamped.
 *  3. Vendor advances past step 1 → markStarted(token).
 *  4. Vendor hits Submit → markSubmitted(token, listingId) — links the invite
 *     to the listing so we can attribute approvals back to the BD agent.
 *  5. Admin approves the listing → markCompleted(token) (called by listening
 *     for service.listing.published Kafka event in V2; for now optional).
 *
 * 30-day TTL — expired invites can't be opened.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorInviteService {

    private final VendorInviteRepository repo;
    private static final SecureRandom RNG = new SecureRandom();

    @Value("${safar.web-url:https://safar.com}")
    private String safarWebUrl;

    @Transactional
    public VendorInvite create(String phone, String serviceType, String businessName,
                               String notes, String sentVia, UUID sentBy) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone required");
        if (serviceType == null || serviceType.isBlank()) throw new IllegalArgumentException("serviceType required");

        String token = generateToken();
        OffsetDateTime now = OffsetDateTime.now();

        VendorInvite invite = VendorInvite.builder()
                .inviteToken(token)
                .phone(phone.trim())
                .serviceType(serviceType)
                .businessName(businessName)
                .notes(notes)
                .sentAt(now)
                .sentVia(sentVia == null ? "MANUAL" : sentVia)
                .sentBy(sentBy)
                .expiresAt(now.plusDays(30))
                .build();

        VendorInvite saved = repo.save(invite);
        log.info("Vendor invite created: token={}, phone={}, type={}, by={}",
                token.substring(0, 8) + "…", phone, serviceType, sentBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<VendorInvite> findActiveByToken(String token) {
        return repo.findByInviteToken(token).filter(this::isActive);
    }

    @Transactional
    public Optional<VendorInvite> markOpened(String token) {
        return repo.findByInviteToken(token).map(invite -> {
            if (!isActive(invite)) return invite;     // expired or cancelled — silently no-op
            if (invite.getOpenedAt() == null) {
                invite.setOpenedAt(OffsetDateTime.now());
                return repo.save(invite);
            }
            return invite;
        });
    }

    @Transactional
    public void markStarted(String token) {
        repo.findByInviteToken(token).ifPresent(invite -> {
            if (isActive(invite) && invite.getOnboardingStartedAt() == null) {
                invite.setOnboardingStartedAt(OffsetDateTime.now());
                repo.save(invite);
            }
        });
    }

    @Transactional
    public void markSubmitted(String token, UUID serviceListingId) {
        repo.findByInviteToken(token).ifPresent(invite -> {
            if (isActive(invite)) {
                invite.setSubmittedAt(OffsetDateTime.now());
                invite.setServiceListingId(serviceListingId);
                repo.save(invite);
            }
        });
    }

    @Transactional
    public void markCompleted(UUID serviceListingId) {
        repo.findAll().stream()
                .filter(i -> serviceListingId.equals(i.getServiceListingId()) && i.getCompletedAt() == null)
                .findFirst()
                .ifPresent(invite -> {
                    invite.setCompletedAt(OffsetDateTime.now());
                    repo.save(invite);
                });
    }

    @Transactional
    public VendorInvite cancel(UUID inviteId) {
        VendorInvite invite = repo.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
        if (invite.getOpenedAt() != null) {
            throw new IllegalStateException("Cannot cancel — vendor already opened the invite");
        }
        invite.setCancelledAt(OffsetDateTime.now());
        return repo.save(invite);
    }

    @Transactional(readOnly = true)
    public List<VendorInvite> listAll() {
        return repo.findAllByOrderBySentAtDesc();
    }

    @Transactional(readOnly = true)
    public List<VendorInvite> listByServiceType(String serviceType) {
        return repo.findByServiceTypeOrderBySentAtDesc(serviceType);
    }

    /** Build the WhatsApp-friendly deep-link URL for this invite. */
    public String buildDeepLink(VendorInvite invite) {
        String typeSlug = serviceTypeSlug(invite.getServiceType());
        return safarWebUrl + "/vendor/onboard/" + typeSlug + "?invite=" + invite.getInviteToken();
    }

    /** Pre-canned WhatsApp message body — admin can copy-paste into their personal WhatsApp. */
    public String buildWhatsAppMessage(VendorInvite invite) {
        String name = (invite.getBusinessName() == null || invite.getBusinessName().isBlank())
                ? "there" : invite.getBusinessName();
        return "Hi " + name + " — start selling on Safar in 10 minutes: "
                + buildDeepLink(invite)
                + "\n\nYour phone is pre-filled. You'll need ID + (for food) FSSAI to publish.";
    }

    private boolean isActive(VendorInvite invite) {
        if (invite.getCancelledAt() != null) return false;
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(OffsetDateTime.now())) return false;
        return true;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String serviceTypeSlug(String serviceType) {
        return switch (serviceType) {
            case "CAKE_DESIGNER" -> "cake";
            case "SINGER" -> "singer";
            case "PANDIT" -> "pandit";
            case "DECORATOR" -> "decor";
            case "STAFF_HIRE" -> "staff-hire";
            default -> serviceType.toLowerCase().replace('_', '-');
        };
    }
}
