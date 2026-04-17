package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.SalePriceHistory;
import com.safar.listing.entity.enums.SalePropertyStatus;
import com.safar.listing.entity.enums.SalePropertyType;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.SalePriceHistoryRepository;
import com.safar.listing.repository.SalePropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePropertyService {

    private final SalePropertyRepository salePropertyRepository;
    private final SalePriceHistoryRepository priceHistoryRepository;
    private final ListingRepository listingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GeocodingService geocodingService;
    private final org.springframework.core.env.Environment env;
    private final ObjectMapper objectMapper;

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    @Transactional
    public SalePropertyResponse create(CreateSalePropertyRequest req, UUID sellerId) {
        SaleProperty sp = SaleProperty.builder()
                .sellerId(sellerId)
                .sellerType(req.sellerType() != null ? req.sellerType() : com.safar.listing.entity.enums.SellerType.OWNER)
                .linkedListingId(req.linkedListingId())
                .title(req.title())
                .description(req.description())
                .salePropertyType(req.salePropertyType())
                .transactionType(req.transactionType() != null ? req.transactionType() : com.safar.listing.entity.enums.TransactionType.RESALE)
                .addressLine1(req.addressLine1())
                .addressLine2(req.addressLine2())
                .locality(req.locality())
                .city(req.city())
                .state(req.state())
                .pincode(req.pincode())
                .lat(req.lat())
                .lng(req.lng())
                .landmark(req.landmark())
                .askingPricePaise(req.askingPricePaise())
                .priceNegotiable(req.priceNegotiable() != null ? req.priceNegotiable() : false)
                .maintenancePaise(req.maintenancePaise())
                .bookingAmountPaise(req.bookingAmountPaise())
                .carpetAreaSqft(req.carpetAreaSqft())
                .builtUpAreaSqft(req.builtUpAreaSqft())
                .superBuiltUpAreaSqft(req.superBuiltUpAreaSqft())
                .plotAreaSqft(req.plotAreaSqft())
                .areaUnit(req.areaUnit() != null ? req.areaUnit() : com.safar.listing.entity.enums.AreaUnit.SQFT)
                .bedrooms(req.bedrooms())
                .bathrooms(req.bathrooms())
                .balconies(req.balconies())
                .floorNumber(req.floorNumber())
                .totalFloors(req.totalFloors())
                .facing(req.facing())
                .propertyAgeYears(req.propertyAgeYears())
                .furnishing(req.furnishing())
                .parkingCovered(req.parkingCovered() != null ? req.parkingCovered() : 0)
                .parkingOpen(req.parkingOpen() != null ? req.parkingOpen() : 0)
                .possessionStatus(req.possessionStatus() != null ? req.possessionStatus() : com.safar.listing.entity.enums.PossessionStatus.READY_TO_MOVE)
                .possessionDate(req.possessionDate())
                .builderName(req.builderName())
                .projectName(req.projectName())
                .reraId(req.reraId())
                .amenities(req.amenities())
                .waterSupply(req.waterSupply())
                .powerBackup(req.powerBackup())
                .gatedCommunity(req.gatedCommunity() != null ? req.gatedCommunity() : false)
                .cornerProperty(req.cornerProperty() != null ? req.cornerProperty() : false)
                .vastuCompliant(req.vastuCompliant() != null ? req.vastuCompliant() : false)
                .petAllowed(req.petAllowed() != null ? req.petAllowed() : false)
                .overlooking(req.overlooking())
                .photos(req.photos())
                .floorPlanUrl(req.floorPlanUrl())
                .videoTourUrl(req.videoTourUrl())
                .brochureUrl(req.brochureUrl())
                .build();

        // Geocode if lat/lng missing
        if (sp.getLat() == null && sp.getPincode() != null) {
            try {
                var coords = geocodingService.geocode(sp.getPincode(), sp.getCity(), sp.getState());
                if (coords != null && coords.length >= 2) {
                    sp.setLat(coords[0]);
                    sp.setLng(coords[1]);
                }
            } catch (Exception e) {
                log.warn("Geocoding failed for sale property: {}", e.getMessage());
            }
        }

        sp = salePropertyRepository.save(sp);

        // Record initial price history
        priceHistoryRepository.save(SalePriceHistory.builder()
                .salePropertyId(sp.getId())
                .pricePaise(sp.getAskingPricePaise())
                .pricePerSqftPaise(sp.getPricePerSqftPaise())
                .build());

        log.info("Sale property created: {} by seller {}", sp.getId(), sellerId);
        return toResponse(sp);
    }

    public Page<SalePropertyResponse> browse(String city, Pageable pageable) {
        Page<SaleProperty> page = salePropertyRepository.findByStatus(SalePropertyStatus.ACTIVE, pageable);
        return page.map(this::toResponse);
    }

    public SalePropertyResponse getById(UUID id) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale property not found: " + id));
        // Increment view count
        sp.setViewsCount(sp.getViewsCount() + 1);
        salePropertyRepository.save(sp);
        return toResponse(sp);
    }

    @Transactional
    public SalePropertyResponse update(UUID id, UpdateSalePropertyRequest req, UUID sellerId) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale property not found: " + id));
        if (!sp.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to update this property");
        }

        Long oldPrice = sp.getAskingPricePaise();

        if (req.title() != null) sp.setTitle(req.title());
        if (req.description() != null) sp.setDescription(req.description());
        if (req.salePropertyType() != null) sp.setSalePropertyType(req.salePropertyType());
        if (req.transactionType() != null) sp.setTransactionType(req.transactionType());
        if (req.sellerType() != null) sp.setSellerType(req.sellerType());
        if (req.addressLine1() != null) sp.setAddressLine1(req.addressLine1());
        if (req.addressLine2() != null) sp.setAddressLine2(req.addressLine2());
        if (req.locality() != null) sp.setLocality(req.locality());
        if (req.city() != null) sp.setCity(req.city());
        if (req.state() != null) sp.setState(req.state());
        if (req.pincode() != null) sp.setPincode(req.pincode());
        if (req.lat() != null) sp.setLat(req.lat());
        if (req.lng() != null) sp.setLng(req.lng());
        if (req.landmark() != null) sp.setLandmark(req.landmark());
        if (req.askingPricePaise() != null) sp.setAskingPricePaise(req.askingPricePaise());
        if (req.priceNegotiable() != null) sp.setPriceNegotiable(req.priceNegotiable());
        if (req.maintenancePaise() != null) sp.setMaintenancePaise(req.maintenancePaise());
        if (req.bookingAmountPaise() != null) sp.setBookingAmountPaise(req.bookingAmountPaise());
        if (req.carpetAreaSqft() != null) sp.setCarpetAreaSqft(req.carpetAreaSqft());
        if (req.builtUpAreaSqft() != null) sp.setBuiltUpAreaSqft(req.builtUpAreaSqft());
        if (req.superBuiltUpAreaSqft() != null) sp.setSuperBuiltUpAreaSqft(req.superBuiltUpAreaSqft());
        if (req.plotAreaSqft() != null) sp.setPlotAreaSqft(req.plotAreaSqft());
        if (req.areaUnit() != null) sp.setAreaUnit(req.areaUnit());
        if (req.bedrooms() != null) sp.setBedrooms(req.bedrooms());
        if (req.bathrooms() != null) sp.setBathrooms(req.bathrooms());
        if (req.balconies() != null) sp.setBalconies(req.balconies());
        if (req.floorNumber() != null) sp.setFloorNumber(req.floorNumber());
        if (req.totalFloors() != null) sp.setTotalFloors(req.totalFloors());
        if (req.facing() != null) sp.setFacing(req.facing());
        if (req.propertyAgeYears() != null) sp.setPropertyAgeYears(req.propertyAgeYears());
        if (req.furnishing() != null) sp.setFurnishing(req.furnishing());
        if (req.parkingCovered() != null) sp.setParkingCovered(req.parkingCovered());
        if (req.parkingOpen() != null) sp.setParkingOpen(req.parkingOpen());
        if (req.possessionStatus() != null) sp.setPossessionStatus(req.possessionStatus());
        if (req.possessionDate() != null) sp.setPossessionDate(req.possessionDate());
        if (req.builderName() != null) sp.setBuilderName(req.builderName());
        if (req.projectName() != null) sp.setProjectName(req.projectName());
        if (req.reraId() != null) sp.setReraId(req.reraId());
        if (req.amenities() != null) sp.setAmenities(req.amenities());
        if (req.waterSupply() != null) sp.setWaterSupply(req.waterSupply());
        if (req.powerBackup() != null) sp.setPowerBackup(req.powerBackup());
        if (req.gatedCommunity() != null) sp.setGatedCommunity(req.gatedCommunity());
        if (req.cornerProperty() != null) sp.setCornerProperty(req.cornerProperty());
        if (req.vastuCompliant() != null) sp.setVastuCompliant(req.vastuCompliant());
        if (req.petAllowed() != null) sp.setPetAllowed(req.petAllowed());
        if (req.overlooking() != null) sp.setOverlooking(req.overlooking());
        if (req.photos() != null) sp.setPhotos(req.photos());
        if (req.floorPlanUrl() != null) sp.setFloorPlanUrl(req.floorPlanUrl());
        if (req.videoTourUrl() != null) sp.setVideoTourUrl(req.videoTourUrl());
        if (req.brochureUrl() != null) sp.setBrochureUrl(req.brochureUrl());

        sp = salePropertyRepository.save(sp);

        // Track price changes
        if (req.askingPricePaise() != null && !req.askingPricePaise().equals(oldPrice)) {
            priceHistoryRepository.save(SalePriceHistory.builder()
                    .salePropertyId(sp.getId())
                    .pricePaise(sp.getAskingPricePaise())
                    .pricePerSqftPaise(sp.getPricePerSqftPaise())
                    .build());

            // Notify buyers who saved this property about price drop
            if (req.askingPricePaise() < oldPrice) {
                kafkaTemplate.send("sale.property.price-drop", sp.getId().toString(), toJson(sp));
            }
        }

        // Re-index in ES
        if (sp.getStatus() == SalePropertyStatus.ACTIVE) {
            kafkaTemplate.send("sale.property.indexed", sp.getId().toString(), toJson(sp));
        }

        return toResponse(sp);
    }

    @Transactional
    /**
     * Host-facing status changes:
     *   PENDING → DRAFT (withdraw before review)
     *   VERIFIED → PAUSED (temporarily hide)
     *   VERIFIED → ARCHIVED (host archives, reversible)
     *   VERIFIED → SOLD (mark sold)
     *   PAUSED → PENDING (re-submit for review)
     *   ARCHIVED → PENDING (restore, re-submit)
     *   DRAFT → PENDING (submit for review)
     *
     * Host CANNOT: VERIFIED (admin only), REJECTED (admin only), SUSPENDED (admin only)
     */
    public SalePropertyResponse updateStatus(UUID id, SalePropertyStatus newStatus, UUID sellerId) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale property not found: " + id));
        if (!sp.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized");
        }

        // Host cannot set admin-only statuses
        if (newStatus == SalePropertyStatus.VERIFIED || newStatus == SalePropertyStatus.REJECTED
                || newStatus == SalePropertyStatus.SUSPENDED) {
            throw new IllegalStateException("Only admin can set status to " + newStatus);
        }

        SalePropertyStatus old = sp.getStatus();
        sp.setStatus(newStatus);
        sp = salePropertyRepository.save(sp);

        // Deindex from ES if was VERIFIED (visible)
        if (old == SalePropertyStatus.VERIFIED && newStatus != SalePropertyStatus.VERIFIED) {
            kafkaTemplate.send("sale.property.deleted", sp.getId().toString(), sp.getId().toString());
        }

        log.info("Sale property {} status changed: {} -> {}", id, old, newStatus);
        return toResponse(sp);
    }

    public Page<SalePropertyResponse> getSellerProperties(UUID sellerId, SalePropertyStatus status, Pageable pageable) {
        Page<SaleProperty> page = status != null
                ? salePropertyRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                : salePropertyRepository.findBySellerId(sellerId, pageable);
        return page.map(this::toResponse);
    }

    public List<SalePropertyResponse> getSimilarProperties(UUID propertyId) {
        SaleProperty sp = salePropertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Not found: " + propertyId));
        return salePropertyRepository.findSimilarProperties(
                sp.getCity(), sp.getSalePropertyType(), sp.getBedrooms(),
                sp.getAskingPricePaise(), propertyId, PageRequest.of(0, 6)
        ).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID id, UUID sellerId) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        if (!sp.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized");
        }
        sp.setStatus(SalePropertyStatus.EXPIRED);
        salePropertyRepository.save(sp);
        kafkaTemplate.send("sale.property.deleted", sp.getId().toString(), sp.getId().toString());
    }

    // Admin methods
    /** Admin verify = approve listing. PENDING → VERIFIED (goes live in search). */
    public SalePropertyResponse adminVerify(UUID id) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        sp.setVerified(true);
        sp.setStatus(SalePropertyStatus.VERIFIED);
        sp.setApprovedAt(java.time.OffsetDateTime.now());
        sp.setExpiresAt(java.time.OffsetDateTime.now().plusDays(90));
        sp = salePropertyRepository.save(sp);
        kafkaTemplate.send("sale.property.indexed", sp.getId().toString(), toJson(sp));
        log.info("Sale property {} verified and activated", id);
        return toResponse(sp);
    }

    public SalePropertyResponse adminVerifyRera(UUID id) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        sp.setReraVerified(true);
        sp = salePropertyRepository.save(sp);
        return toResponse(sp);
    }

    public SalePropertyResponse adminSuspend(UUID id) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        sp.setStatus(SalePropertyStatus.SUSPENDED);
        sp = salePropertyRepository.save(sp);
        kafkaTemplate.send("sale.property.deleted", sp.getId().toString(), sp.getId().toString());
        return toResponse(sp);
    }

    /**
     * Admin: set any status.
     * PENDING → VERIFIED (approve), PENDING → REJECTED (reject)
     * Any → SUSPENDED (policy), SUSPENDED → VERIFIED (unsuspend)
     * VERIFIED = visible in search (indexed in ES)
     */
    @Transactional
    public SalePropertyResponse adminSetStatus(UUID id, SalePropertyStatus newStatus) {
        SaleProperty sp = salePropertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        SalePropertyStatus old = sp.getStatus();
        sp.setStatus(newStatus);

        // Set approval timestamp when verifying
        if (newStatus == SalePropertyStatus.VERIFIED && old != SalePropertyStatus.VERIFIED) {
            sp.setApprovedAt(java.time.OffsetDateTime.now());
            sp.setExpiresAt(java.time.OffsetDateTime.now().plusDays(90));
        }

        sp = salePropertyRepository.save(sp);

        // VERIFIED = visible in search → index in ES
        if (newStatus == SalePropertyStatus.VERIFIED) {
            kafkaTemplate.send("sale.property.indexed", sp.getId().toString(), toJson(sp));
        } else if (old == SalePropertyStatus.VERIFIED) {
            // Was visible, now hidden → remove from ES
            kafkaTemplate.send("sale.property.deleted", sp.getId().toString(), sp.getId().toString());
        }

        log.info("Admin changed sale property {} status: {} -> {}", id, old, newStatus);
        return toResponse(sp);
    }

    public Page<Map<String, Object>> adminList(SalePropertyStatus status, Pageable pageable) {
        Page<SaleProperty> properties = (status != null)
                ? salePropertyRepository.findByStatus(status, pageable)
                : salePropertyRepository.findAll(pageable);

        Set<UUID> sellerIds = new HashSet<>();
        properties.forEach(sp -> sellerIds.add(sp.getSellerId()));
        Map<UUID, Map<String, String>> contacts = fetchUserContacts(sellerIds);

        return properties.map(sp -> {
            SalePropertyResponse resp = toResponse(sp);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", resp.id());
            map.put("sellerId", resp.sellerId());
            map.put("sellerType", resp.sellerType());
            map.put("title", resp.title());
            map.put("description", resp.description());
            map.put("salePropertyType", resp.salePropertyType());
            map.put("transactionType", resp.transactionType());
            map.put("locality", resp.locality());
            map.put("city", resp.city());
            map.put("state", resp.state());
            map.put("pincode", resp.pincode());
            map.put("askingPricePaise", resp.askingPricePaise());
            map.put("pricePerSqftPaise", resp.pricePerSqftPaise());
            map.put("priceNegotiable", resp.priceNegotiable());
            map.put("carpetAreaSqft", resp.carpetAreaSqft());
            map.put("builtUpAreaSqft", resp.builtUpAreaSqft());
            map.put("bedrooms", resp.bedrooms());
            map.put("bathrooms", resp.bathrooms());
            map.put("floorNumber", resp.floorNumber());
            map.put("totalFloors", resp.totalFloors());
            map.put("facing", resp.facing());
            map.put("furnishing", resp.furnishing());
            map.put("possessionStatus", resp.possessionStatus());
            map.put("builderName", resp.builderName());
            map.put("projectName", resp.projectName());
            map.put("reraId", resp.reraId());
            map.put("reraVerified", resp.reraVerified());
            map.put("status", resp.status());
            map.put("verified", resp.verified());
            map.put("viewsCount", resp.viewsCount());
            map.put("inquiriesCount", resp.inquiriesCount());
            map.put("createdAt", resp.createdAt());
            map.put("updatedAt", resp.updatedAt());
            // Enriched seller contact
            Map<String, String> contact = contacts.getOrDefault(sp.getSellerId(), Map.of());
            map.put("sellerName", contact.getOrDefault("name", ""));
            map.put("sellerPhone", contact.getOrDefault("phone", ""));
            map.put("sellerEmail", contact.getOrDefault("email", ""));
            return map;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<String, String>> fetchUserContacts(Set<UUID> userIds) {
        Map<UUID, Map<String, String>> result = new HashMap<>();
        String userUrl = env.getProperty("services.user-service.url");
        if (userUrl == null || userIds.isEmpty()) return result;
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        for (UUID uid : userIds) {
            try {
                Map<String, String> contact = rt.getForObject(
                        userUrl + "/api/v1/internal/users/" + uid + "/contact", Map.class);
                if (contact != null) result.put(uid, contact);
            } catch (Exception e) {
                log.debug("Failed to fetch contact for user {}: {}", uid, e.getMessage());
            }
        }
        return result;
    }

    // Scheduled: expire old properties
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void expireOldProperties() {
        List<SaleProperty> expired = salePropertyRepository.findExpiredProperties();
        for (SaleProperty sp : expired) {
            sp.setStatus(SalePropertyStatus.EXPIRED);
            salePropertyRepository.save(sp);
            kafkaTemplate.send("sale.property.deleted", sp.getId().toString(), sp.getId().toString());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} sale properties", expired.size());
        }
    }

    // Reindex all active — called by admin endpoint and on startup
    public int reindexAll() {
        List<SaleProperty> active = salePropertyRepository.findByStatus(SalePropertyStatus.ACTIVE);
        for (SaleProperty sp : active) {
            try {
                kafkaTemplate.send("sale.property.indexed", sp.getId().toString(), toJson(sp));
            } catch (Exception e) {
                log.warn("Failed to index sale property {}: {}", sp.getId(), e.getMessage());
            }
        }
        log.info("Reindexed {} active sale properties", active.size());
        return active.size();
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onStartupReindex() {
        try {
            int count = reindexAll();
            log.info("Startup sale property reindex: {} properties", count);
        } catch (Exception e) {
            log.warn("Startup sale property reindex failed: {}", e.getMessage());
        }
    }

    public java.util.Map<String, String> getSellerContact(UUID propertyId) {
        SaleProperty sp = salePropertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            String userUrl = env.getProperty("services.user-service.url");
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> contact = rt.getForObject(
                    userUrl + "/api/v1/internal/users/" + sp.getSellerId() + "/contact", java.util.Map.class);
            if (contact != null) {
                return java.util.Map.of(
                        "name", contact.getOrDefault("name", ""),
                        "phone", contact.getOrDefault("phone", "Not available"),
                        "email", contact.getOrDefault("email", "")
                );
            }
        } catch (Exception e) {
            log.warn("Failed to fetch seller contact: {}", e.getMessage());
        }
        return java.util.Map.of("name", "", "phone", "Not available", "email", "");
    }

    private SalePropertyResponse toResponse(SaleProperty sp) {
        // Fetch linked listing rental data if available
        Double linkedRating = null;
        Integer linkedReviewCount = null;
        Long linkedRentPaise = null;

        if (sp.getLinkedListingId() != null) {
            try {
                Listing linked = listingRepository.findById(sp.getLinkedListingId()).orElse(null);
                if (linked != null) {
                    linkedRating = linked.getAvgRating();
                    linkedReviewCount = linked.getReviewCount();
                    linkedRentPaise = linked.getBasePricePaise();
                }
            } catch (Exception e) {
                log.warn("Could not fetch linked listing: {}", e.getMessage());
            }
        }

        return new SalePropertyResponse(
                sp.getId(), sp.getSellerId(), sp.getSellerType(), sp.getLinkedListingId(),
                sp.getTitle(), sp.getDescription(), sp.getSalePropertyType(), sp.getTransactionType(),
                sp.getAddressLine1(), sp.getAddressLine2(), sp.getLocality(),
                sp.getCity(), sp.getState(), sp.getPincode(),
                sp.getLat(), sp.getLng(), sp.getLandmark(),
                sp.getAskingPricePaise(), sp.getPricePerSqftPaise(), sp.getPriceNegotiable(),
                sp.getMaintenancePaise(), sp.getBookingAmountPaise(),
                sp.getCarpetAreaSqft(), sp.getBuiltUpAreaSqft(), sp.getSuperBuiltUpAreaSqft(),
                sp.getPlotAreaSqft(), sp.getAreaUnit(),
                sp.getBedrooms(), sp.getBathrooms(), sp.getBalconies(),
                sp.getFloorNumber(), sp.getTotalFloors(), sp.getFacing(),
                sp.getPropertyAgeYears(), sp.getFurnishing(),
                sp.getParkingCovered(), sp.getParkingOpen(),
                sp.getPossessionStatus(), sp.getPossessionDate(),
                sp.getBuilderName(), sp.getProjectName(), sp.getReraId(), sp.getReraVerified(),
                sp.getAmenities(), sp.getWaterSupply(), sp.getPowerBackup(),
                sp.getGatedCommunity(), sp.getCornerProperty(), sp.getVastuCompliant(),
                sp.getPetAllowed(), sp.getOverlooking(),
                sp.getPhotos(), sp.getFloorPlanUrl(), sp.getVideoTourUrl(), sp.getBrochureUrl(),
                sp.getStatus(), sp.getFeatured(), sp.getVerified(),
                sp.getViewsCount(), sp.getInquiriesCount(), sp.getExpiresAt(),
                linkedRating, linkedReviewCount, linkedRentPaise,
                sp.getCreatedAt(), sp.getUpdatedAt(), sp.getApprovedAt()
        );
    }
}
