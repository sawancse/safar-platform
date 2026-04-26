package com.safar.chef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.chef.dto.CreateServiceListingRequest;
import com.safar.chef.dto.UpdateServiceListingRequest;
import com.safar.chef.entity.*;
import com.safar.chef.entity.enums.ServiceListingStatus;
import com.safar.chef.entity.enums.ServiceListingType;
import com.safar.chef.repository.ServiceListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lifecycle manager for service_listings.
 *
 * State machine:
 *   DRAFT  -- submit -->  PENDING_REVIEW  (KYC gate enforced)
 *   PENDING_REVIEW  -- approve -->  VERIFIED
 *   PENDING_REVIEW  -- reject  -->  DRAFT (with reason)
 *   VERIFIED  -- pause (vendor)  --> PAUSED
 *   PAUSED    -- resume         --> VERIFIED
 *   any       -- suspend (admin) --> SUSPENDED
 *   SUSPENDED -- restore (admin) --> DRAFT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceListingService {

    private final ServiceListingRepository repo;
    private final ServiceListingPublishValidator publishValidator;
    private final ObjectMapper objectMapper;
    private final ResilientKafkaService kafka;

    // ── Create / Update ─────────────────────────────────────

    @Transactional
    public ServiceListing create(CreateServiceListingRequest req, UUID vendorUserId) {
        if (req.serviceType() == null) throw new IllegalArgumentException("serviceType required");
        if (req.businessName() == null || req.businessName().isBlank())
            throw new IllegalArgumentException("businessName required");
        if (req.vendorSlug() == null || req.vendorSlug().isBlank())
            throw new IllegalArgumentException("vendorSlug required");
        if (repo.existsByVendorSlug(req.vendorSlug()))
            throw new IllegalArgumentException("vendorSlug already taken: " + req.vendorSlug());

        ServiceListingType type;
        try {
            type = ServiceListingType.valueOf(req.serviceType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown serviceType: " + req.serviceType());
        }

        Map<String, Object> combined = sharedFields(req);
        combined.put("vendorUserId", vendorUserId);
        combined.put("status", ServiceListingStatus.DRAFT.name());
        combined.put("trustTier", "LISTED");
        combined.put("ratingCount", 0);
        combined.put("completedBookingsCount", 0);
        if (req.typeAttributes() != null) combined.putAll(req.typeAttributes());

        ServiceListing entity = objectMapper.convertValue(combined, entityClassFor(type));
        ServiceListing saved = repo.save(entity);
        log.info("Created DRAFT service listing {} type={} vendor={}",
                saved.getId(), type, vendorUserId);
        return saved;
    }

    @Transactional
    public ServiceListing update(UUID listingId, UpdateServiceListingRequest req, UUID vendorUserId) {
        ServiceListing existing = mustOwn(listingId, vendorUserId);
        if (existing.getStatus() != ServiceListingStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT listings can be edited; current=" + existing.getStatus());
        }

        // Apply non-null fields onto the existing entity. Use objectMapper.updateValue
        // for type-specific fields so the right child setters fire.
        Map<String, Object> patch = sharedFieldsFromUpdate(req);
        if (req.typeAttributes() != null) patch.putAll(req.typeAttributes());

        try {
            objectMapper.updateValue(existing, patch);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not apply update: " + e.getMessage(), e);
        }

        ServiceListing saved = repo.save(existing);
        log.info("Updated DRAFT service listing {} by vendor {}", saved.getId(), vendorUserId);
        return saved;
    }

    private Class<? extends ServiceListing> entityClassFor(ServiceListingType type) {
        return switch (type) {
            case CAKE_DESIGNER -> CakeAttributes.class;
            case SINGER -> SingerAttributes.class;
            case PANDIT -> PanditAttributes.class;
            case DECORATOR -> DecorAttributes.class;
            case STAFF_HIRE -> StaffAttributes.class;
            // V2 types fall back to parent until child tables are added
            default -> throw new IllegalArgumentException(
                    "service_type " + type + " not yet supported in MVP");
        };
    }

    private Map<String, Object> sharedFields(CreateServiceListingRequest req) {
        Map<String, Object> m = new HashMap<>();
        if (req.businessName() != null) m.put("businessName", req.businessName());
        if (req.vendorSlug() != null) m.put("vendorSlug", req.vendorSlug());
        if (req.heroImageUrl() != null) m.put("heroImageUrl", req.heroImageUrl());
        if (req.tagline() != null) m.put("tagline", req.tagline());
        if (req.aboutMd() != null) m.put("aboutMd", req.aboutMd());
        if (req.foundedYear() != null) m.put("foundedYear", req.foundedYear());
        if (req.cities() != null) m.put("cities", req.cities());
        if (req.homeCity() != null) m.put("homeCity", req.homeCity());
        if (req.homePincode() != null) m.put("homePincode", req.homePincode());
        if (req.homeAddress() != null) m.put("homeAddress", req.homeAddress());
        if (req.homeLat() != null) m.put("homeLat", req.homeLat());
        if (req.homeLng() != null) m.put("homeLng", req.homeLng());
        if (req.deliveryRadiusKm() != null) m.put("deliveryRadiusKm", req.deliveryRadiusKm());
        if (req.outstationCapable() != null) m.put("outstationCapable", req.outstationCapable());
        if (req.deliveryChannels() != null) m.put("deliveryChannels", req.deliveryChannels());
        if (req.pricingPattern() != null) m.put("pricingPattern", req.pricingPattern());
        if (req.pricingFormula() != null) m.put("pricingFormula", req.pricingFormula());
        if (req.calendarMode() != null) m.put("calendarMode", req.calendarMode());
        if (req.defaultLeadTimeHours() != null) m.put("defaultLeadTimeHours", req.defaultLeadTimeHours());
        if (req.cancellationPolicy() != null) m.put("cancellationPolicy", req.cancellationPolicy());
        if (req.cancellationTermsMd() != null) m.put("cancellationTermsMd", req.cancellationTermsMd());
        return m;
    }

    private Map<String, Object> sharedFieldsFromUpdate(UpdateServiceListingRequest req) {
        Map<String, Object> m = new HashMap<>();
        if (req.businessName() != null) m.put("businessName", req.businessName());
        if (req.vendorSlug() != null) m.put("vendorSlug", req.vendorSlug());
        if (req.heroImageUrl() != null) m.put("heroImageUrl", req.heroImageUrl());
        if (req.tagline() != null) m.put("tagline", req.tagline());
        if (req.aboutMd() != null) m.put("aboutMd", req.aboutMd());
        if (req.foundedYear() != null) m.put("foundedYear", req.foundedYear());
        if (req.cities() != null) m.put("cities", req.cities());
        if (req.homeCity() != null) m.put("homeCity", req.homeCity());
        if (req.homePincode() != null) m.put("homePincode", req.homePincode());
        if (req.homeAddress() != null) m.put("homeAddress", req.homeAddress());
        if (req.homeLat() != null) m.put("homeLat", req.homeLat());
        if (req.homeLng() != null) m.put("homeLng", req.homeLng());
        if (req.deliveryRadiusKm() != null) m.put("deliveryRadiusKm", req.deliveryRadiusKm());
        if (req.outstationCapable() != null) m.put("outstationCapable", req.outstationCapable());
        if (req.deliveryChannels() != null) m.put("deliveryChannels", req.deliveryChannels());
        if (req.pricingPattern() != null) m.put("pricingPattern", req.pricingPattern());
        if (req.pricingFormula() != null) m.put("pricingFormula", req.pricingFormula());
        if (req.calendarMode() != null) m.put("calendarMode", req.calendarMode());
        if (req.defaultLeadTimeHours() != null) m.put("defaultLeadTimeHours", req.defaultLeadTimeHours());
        if (req.cancellationPolicy() != null) m.put("cancellationPolicy", req.cancellationPolicy());
        if (req.cancellationTermsMd() != null) m.put("cancellationTermsMd", req.cancellationTermsMd());
        return m;
    }

    // ── Reads ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ServiceListing get(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service listing not found: " + id));
    }

    @Transactional(readOnly = true)
    public ServiceListing getBySlug(String slug) {
        return repo.findByVendorSlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Service listing not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<ServiceListing> listByVendor(UUID vendorUserId) {
        return repo.findByVendorUserId(vendorUserId);
    }

    @Transactional(readOnly = true)
    public List<ServiceListing> listByStatus(ServiceListingStatus status) {
        return repo.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<ServiceListing> listVerified(String serviceType, String city) {
        return listVerified(serviceType, city, null);
    }

    /**
     * Search VERIFIED listings, optionally filtered by service type, city, and
     * a date the vendor must be available on (Sprint 4 — date-availability
     * filter, competitive differentiator from The Knot).
     */
    @Transactional(readOnly = true)
    public List<ServiceListing> listVerified(String serviceType, String city, java.time.LocalDate availableOn) {
        if (availableOn != null) {
            return repo.findVerifiedAvailableOn(availableOn,
                    serviceType,
                    (city != null && !city.isBlank()) ? city.trim() : null);
        }
        if (serviceType != null && city != null && !city.isBlank()) {
            return repo.findVerifiedByTypeAndCity(serviceType, city.trim());
        }
        if (serviceType != null) {
            return repo.findByServiceTypeAndStatus(serviceType, ServiceListingStatus.VERIFIED);
        }
        return repo.findByStatus(ServiceListingStatus.VERIFIED);
    }

    // ── Lifecycle transitions ───────────────────────────────

    @Transactional
    public ServiceListing submit(UUID listingId, UUID vendorUserId) {
        ServiceListing listing = mustOwn(listingId, vendorUserId);
        if (listing.getStatus() != ServiceListingStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT listings can be submitted; current=" + listing.getStatus());
        }
        publishValidator.validateOrThrow(listing);
        return transitionTo(listing, ServiceListingStatus.PENDING_REVIEW, vendorUserId, null);
    }

    @Transactional
    public ServiceListing approve(UUID listingId, UUID adminUserId) {
        ServiceListing listing = get(listingId);
        if (listing.getStatus() != ServiceListingStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Only PENDING_REVIEW listings can be approved; current=" + listing.getStatus());
        }
        return transitionTo(listing, ServiceListingStatus.VERIFIED, adminUserId, null);
    }

    @Transactional
    public ServiceListing reject(UUID listingId, UUID adminUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason required");
        }
        ServiceListing listing = get(listingId);
        if (listing.getStatus() != ServiceListingStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Only PENDING_REVIEW listings can be rejected; current=" + listing.getStatus());
        }
        return transitionTo(listing, ServiceListingStatus.DRAFT, adminUserId, reason);
    }

    @Transactional
    public ServiceListing pause(UUID listingId, UUID vendorUserId) {
        ServiceListing listing = mustOwn(listingId, vendorUserId);
        if (listing.getStatus() != ServiceListingStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only VERIFIED listings can be paused; current=" + listing.getStatus());
        }
        return transitionTo(listing, ServiceListingStatus.PAUSED, vendorUserId, null);
    }

    @Transactional
    public ServiceListing resume(UUID listingId, UUID vendorUserId) {
        ServiceListing listing = mustOwn(listingId, vendorUserId);
        if (listing.getStatus() != ServiceListingStatus.PAUSED) {
            throw new IllegalStateException(
                    "Only PAUSED listings can be resumed; current=" + listing.getStatus());
        }
        return transitionTo(listing, ServiceListingStatus.VERIFIED, vendorUserId, null);
    }

    @Transactional
    public ServiceListing suspend(UUID listingId, UUID adminUserId, String reason) {
        ServiceListing listing = get(listingId);
        if (listing.getStatus() == ServiceListingStatus.SUSPENDED) {
            throw new IllegalStateException("Listing already suspended");
        }
        return transitionTo(listing, ServiceListingStatus.SUSPENDED, adminUserId, reason);
    }

    @Transactional
    public ServiceListing restore(UUID listingId, UUID adminUserId) {
        ServiceListing listing = get(listingId);
        if (listing.getStatus() != ServiceListingStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Only SUSPENDED listings can be restored; current=" + listing.getStatus());
        }
        return transitionTo(listing, ServiceListingStatus.DRAFT, adminUserId, null);
    }

    /** Admin-only escape hatch for metadata edits (e.g. commission override). */
    @Transactional
    public ServiceListing adminSaveListing(ServiceListing listing) {
        return repo.save(listing);
    }

    // ── Helpers ─────────────────────────────────────────────

    private ServiceListing mustOwn(UUID listingId, UUID vendorUserId) {
        ServiceListing listing = get(listingId);
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Listing belongs to another vendor");
        }
        return listing;
    }

    private ServiceListing transitionTo(ServiceListing listing, ServiceListingStatus next,
                                        UUID changedBy, String reason) {
        ServiceListingStatus prev = listing.getStatus();
        listing.setStatus(next);
        listing.setStatusChangedAt(OffsetDateTime.now());
        listing.setStatusChangedBy(changedBy);
        listing.setRejectionReason(reason);
        ServiceListing saved = repo.save(listing);
        log.info("Service listing {} transitioned {} -> {} by {}",
                listing.getId(), prev, next, changedBy);
        publishLifecycleEvent(saved, prev, next);
        return saved;
    }

    /**
     * Publishes a Kafka event when a listing reaches a notable lifecycle state.
     * Consumers: notification-service (vendor email/SMS), search-service (index/de-index),
     * booking-service (validate available bookings on suspend).
     */
    private void publishLifecycleEvent(ServiceListing saved, ServiceListingStatus prev, ServiceListingStatus next) {
        String topic;
        switch (next) {
            case VERIFIED -> topic = (prev == ServiceListingStatus.PAUSED) ? "service.listing.resumed" : "service.listing.published";
            case SUSPENDED -> topic = "service.listing.suspended";
            case PAUSED -> topic = "service.listing.paused";
            case DRAFT -> {
                // DRAFT can be reached from PENDING_REVIEW (rejection) or SUSPENDED (admin restore).
                // Only the rejection path is interesting to subscribers (vendor notification).
                if (prev == ServiceListingStatus.PENDING_REVIEW) topic = "service.listing.rejected";
                else return;
            }
            case PENDING_REVIEW -> topic = "service.listing.submitted";
            default -> { return; }
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", saved.getId().toString());
            payload.put("vendorUserId", saved.getVendorUserId().toString());
            payload.put("serviceType", saved.getServiceType());
            payload.put("businessName", saved.getBusinessName());
            payload.put("vendorSlug", saved.getVendorSlug());
            payload.put("status", next.name());
            payload.put("previousStatus", prev.name());
            if (saved.getRejectionReason() != null) payload.put("reason", saved.getRejectionReason());
            payload.put("at", OffsetDateTime.now().toString());

            kafka.send(topic, saved.getId().toString(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // Outbox already takes care of retries; this catch is just for serialization issues
            log.error("Failed to publish lifecycle event {} for listing {}: {}", topic, saved.getId(), e.getMessage());
        }
    }
}
