package com.safar.listing.service;

import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.dto.ListingResponse;
import com.safar.listing.dto.RoomTypeRequest;
import com.safar.listing.dto.UpdateListingRequest;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.HostTier;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.PricingUnit;
import com.safar.listing.entity.enums.MediaType;
import com.safar.listing.entity.enums.ArchiveReason;
import com.safar.listing.entity.enums.ModerationStatus;
import com.safar.listing.entity.HospitalPartner;
import com.safar.listing.entity.ListingMedia;
import com.safar.listing.entity.MedicalStayPackage;
import com.safar.listing.entity.RoomType;
import com.safar.listing.entity.enums.RoomVariant;
import com.safar.listing.entity.enums.SharingType;
import com.safar.listing.entity.enums.StayMode;
import com.safar.listing.repository.HospitalPartnerRepository;
import com.safar.listing.repository.ListingMediaRepository;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.MedicalStayPackageRepository;
import com.safar.listing.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final MedicalStayPackageRepository medicalStayPackageRepository;
    private final HospitalPartnerRepository hospitalPartnerRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final SubscriptionTierClient subscriptionTierClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final GeocodingService geocodingService;

    @Transactional
    public ListingResponse createListing(UUID hostId, CreateListingRequest req) {
        // 1. Validate commercial type
        if (req.type() == ListingType.COMMERCIAL && req.commercialCategory() == null) {
            throw new IllegalArgumentException("commercialCategory is required for COMMERCIAL listings");
        }
        if (req.type() != ListingType.COMMERCIAL && req.commercialCategory() != null) {
            throw new IllegalArgumentException("commercialCategory must be null for non-COMMERCIAL listings");
        }

        // 2. Check subscription tier
        HostTier tier = subscriptionTierClient.getTier(hostId);

        if (req.type() == ListingType.COMMERCIAL && tier != HostTier.COMMERCIAL) {
            throw new IllegalArgumentException(
                    "COMMERCIAL listings require the Commercial subscription plan");
        }

        long activeCount = listingRepository.findByHostId(hostId).stream()
                .filter(l -> l.getStatus() != ListingStatus.DRAFT)
                .count();
        if (activeCount >= tier.getMaxListings()) {
            throw new IllegalStateException(
                    "Listing limit reached for your plan (" + tier.getMaxListings() +
                    " listings). Please upgrade your subscription.");
        }

        // 3. Pricing unit: COMMERCIAL=HOUR, PG/COLIVING=MONTH, else default NIGHT
        PricingUnit pricingUnit = req.pricingUnit() != null ? req.pricingUnit() : PricingUnit.NIGHT;
        if (req.type() == ListingType.COMMERCIAL) {
            pricingUnit = PricingUnit.HOUR;
        } else if (req.type() == ListingType.PG || req.type() == ListingType.COLIVING) {
            pricingUnit = PricingUnit.MONTH;
        }

        Listing listing = Listing.builder()
                .hostId(hostId)
                .title(req.title())
                .description(req.description())
                .type(req.type())
                .commercialCategory(req.commercialCategory())
                .addressLine1(req.addressLine1())
                .addressLine2(req.addressLine2())
                .city(req.city())
                .state(req.state())
                .pincode(req.pincode())
                .lat(req.lat())
                .lng(req.lng())
                .maxGuests(req.maxGuests())
                .bedrooms(req.bedrooms())
                .bathrooms(req.bathrooms())
                .totalRooms(req.totalRooms() != null ? req.totalRooms() : 1)
                .amenities(req.amenities())
                .basePricePaise(req.basePricePaise())
                .pricingUnit(pricingUnit)
                .minBookingHours(req.minBookingHours() != null ? req.minBookingHours() : 1)
                .instantBook(req.instantBook() != null && req.instantBook())
                .gstApplicable(req.gstApplicable() != null ? req.gstApplicable() : true)
                .gstin(req.gstin())
                .petFriendly(req.petFriendly() != null ? req.petFriendly() : false)
                .maxPets(req.maxPets() != null ? req.maxPets() : 0)
                .starRating(req.starRating())
                .cancellationPolicy(req.cancellationPolicy() != null ? req.cancellationPolicy() : com.safar.listing.entity.enums.CancellationPolicy.MODERATE)
                .mealPlan(req.mealPlan() != null ? req.mealPlan() : com.safar.listing.entity.enums.MealPlan.NONE)
                .bedTypes(req.bedTypes())
                .accessibilityFeatures(req.accessibilityFeatures())
                .freeCancellation(req.freeCancellation() != null ? req.freeCancellation() : false)
                .noPrepayment(req.noPrepayment() != null ? req.noPrepayment() : false)
                .checkInFrom(parseTime(req.checkInFrom(), "14:00"))
                .checkInUntil(parseTime(req.checkInUntil(), "23:00"))
                .checkOutFrom(parseTime(req.checkOutFrom(), "06:00"))
                .checkOutUntil(parseTime(req.checkOutUntil(), "11:00"))
                .childrenAllowed(req.childrenAllowed() != null ? req.childrenAllowed() : true)
                .parkingType(req.parkingType() != null ? req.parkingType() : com.safar.listing.entity.enums.ParkingType.NONE)
                .breakfastIncluded(req.breakfastIncluded() != null ? req.breakfastIncluded() : false)
                .areaSqft(req.areaSqft())
                .operatingHoursFrom(parseTime(req.operatingHoursFrom(), null))
                .operatingHoursUntil(parseTime(req.operatingHoursUntil(), null))
                .aashrayReady(req.aashrayReady() != null ? req.aashrayReady() : false)
                .aashrayDiscountPercent(req.aashrayDiscountPercent() != null ? req.aashrayDiscountPercent() : 0)
                .longTermMonthlyPaise(req.longTermMonthlyPaise())
                .minStayDays(req.minStayDays() != null ? req.minStayDays() : 1)
                .floorPlanUrl(req.floorPlanUrl())
                .panoramaUrl(req.panoramaUrl())
                .videoTourUrl(req.videoTourUrl())
                .neighborhoodPhotoUrls(req.neighborhoodPhotoUrls())
                .weeklyDiscountPercent(req.weeklyDiscountPercent())
                .monthlyDiscountPercent(req.monthlyDiscountPercent())
                .cleaningFeePaise(req.cleaningFeePaise() != null ? req.cleaningFeePaise() : 0L)
                .visibilityBoostPercent(validateVisibilityBoost(req.visibilityBoostPercent()))
                // PG/Co-living fields
                .occupancyType(req.occupancyType())
                .foodType(req.foodType())
                .gateClosingTime(parseTime(req.gateClosingTime(), null))
                .noticePeriodDays(req.noticePeriodDays())
                .securityDepositPaise(req.securityDepositPaise())
                .depositType(req.depositType())
                .depositTerms(req.depositTerms())
                .gracePeriodDays(req.gracePeriodDays() != null ? req.gracePeriodDays() : 5)
                .latePenaltyBps(req.latePenaltyBps() != null ? req.latePenaltyBps() : 200)
                // Insurance
                .insuranceEnabled(req.insuranceEnabled() != null ? req.insuranceEnabled() : false)
                .insuranceAmountPaise(req.insuranceAmountPaise())
                .insuranceType(req.insuranceType())
                // Hotel fields
                .hotelChain(req.hotelChain())
                .frontDesk24h(req.frontDesk24h() != null ? req.frontDesk24h() : false)
                .checkoutTime(parseTime(req.checkoutTime(), null))
                .checkinTime(parseTime(req.checkinTime(), null))
                .status(ListingStatus.DRAFT)
                .build();

        // Auto-geocode if lat/lng are zero (not provided by frontend)
        if (listing.getLat().compareTo(BigDecimal.ZERO) == 0
                && listing.getLng().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal[] coords = geocodingService.geocode(
                    listing.getPincode(), listing.getCity(), listing.getState());
            if (coords != null) {
                listing.setLat(coords[0]);
                listing.setLng(coords[1]);
            }
        }

        Listing saved = listingRepository.save(listing);

        // PG/COLIVING room types: use wizard-provided list if present, else seed a default
        if (saved.getType() == ListingType.PG || saved.getType() == ListingType.COLIVING) {
            if (req.roomTypes() != null && !req.roomTypes().isEmpty()) {
                createWizardRoomTypes(saved, req.roomTypes());
            } else {
                seedDefaultPgRoomType(saved);
            }
        }

        // Publish host.registered event on first listing (async, non-blocking)
        long totalCount = listingRepository.findByHostId(hostId).size();
        if (totalCount == 1) {
            try {
                String event = String.format("{\"hostId\":\"%s\"}", hostId);
                kafkaTemplate.send("host.registered", hostId.toString(), event);
                log.info("First listing created by host {} — host.registered event published", hostId);
            } catch (Exception e) {
                log.warn("Failed to publish host.registered event for host {}: {}", hostId, e.getMessage());
            }
        }

        return toResponse(saved);
    }

    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse updateListing(UUID listingId, UUID hostId, UpdateListingRequest req) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT listings can be edited. Pause the listing first.");
        }

        if (req.title() != null) listing.setTitle(req.title());
        if (req.description() != null) listing.setDescription(req.description());
        if (req.addressLine1() != null) listing.setAddressLine1(req.addressLine1());
        if (req.addressLine2() != null) listing.setAddressLine2(req.addressLine2());
        if (req.city() != null) listing.setCity(req.city());
        if (req.state() != null) listing.setState(req.state());
        if (req.pincode() != null) listing.setPincode(req.pincode());
        if (req.lat() != null) listing.setLat(req.lat());
        if (req.lng() != null) listing.setLng(req.lng());
        if (req.maxGuests() != null) listing.setMaxGuests(req.maxGuests());
        if (req.bedrooms() != null) listing.setBedrooms(req.bedrooms());
        if (req.bathrooms() != null) listing.setBathrooms(req.bathrooms());
        if (req.totalRooms() != null) listing.setTotalRooms(req.totalRooms());
        if (req.amenities() != null) listing.setAmenities(req.amenities());
        if (req.basePricePaise() != null) listing.setBasePricePaise(req.basePricePaise());
        if (req.pricingUnit() != null) listing.setPricingUnit(req.pricingUnit());
        if (req.minBookingHours() != null) listing.setMinBookingHours(req.minBookingHours());
        if (req.instantBook() != null) listing.setInstantBook(req.instantBook());
        if (req.gstApplicable() != null) listing.setGstApplicable(req.gstApplicable());
        if (req.gstin() != null) listing.setGstin(req.gstin());
        if (req.petFriendly() != null) listing.setPetFriendly(req.petFriendly());
        if (req.maxPets() != null) listing.setMaxPets(req.maxPets());
        if (req.starRating() != null) listing.setStarRating(req.starRating());
        if (req.cancellationPolicy() != null) listing.setCancellationPolicy(req.cancellationPolicy());
        if (req.mealPlan() != null) listing.setMealPlan(req.mealPlan());
        if (req.bedTypes() != null) listing.setBedTypes(req.bedTypes());
        if (req.accessibilityFeatures() != null) listing.setAccessibilityFeatures(req.accessibilityFeatures());
        if (req.freeCancellation() != null) listing.setFreeCancellation(req.freeCancellation());
        if (req.noPrepayment() != null) listing.setNoPrepayment(req.noPrepayment());
        if (req.checkInFrom() != null) listing.setCheckInFrom(java.time.LocalTime.parse(req.checkInFrom()));
        if (req.checkInUntil() != null) listing.setCheckInUntil(java.time.LocalTime.parse(req.checkInUntil()));
        if (req.checkOutFrom() != null) listing.setCheckOutFrom(java.time.LocalTime.parse(req.checkOutFrom()));
        if (req.checkOutUntil() != null) listing.setCheckOutUntil(java.time.LocalTime.parse(req.checkOutUntil()));
        if (req.childrenAllowed() != null) listing.setChildrenAllowed(req.childrenAllowed());
        if (req.parkingType() != null) listing.setParkingType(req.parkingType());
        if (req.breakfastIncluded() != null) listing.setBreakfastIncluded(req.breakfastIncluded());
        if (req.areaSqft() != null) listing.setAreaSqft(req.areaSqft());
        if (req.operatingHoursFrom() != null) listing.setOperatingHoursFrom(java.time.LocalTime.parse(req.operatingHoursFrom()));
        if (req.operatingHoursUntil() != null) listing.setOperatingHoursUntil(java.time.LocalTime.parse(req.operatingHoursUntil()));
        if (req.aashrayReady() != null) listing.setAashrayReady(req.aashrayReady());
        if (req.aashrayDiscountPercent() != null) listing.setAashrayDiscountPercent(req.aashrayDiscountPercent());
        if (req.longTermMonthlyPaise() != null) listing.setLongTermMonthlyPaise(req.longTermMonthlyPaise());
        if (req.minStayDays() != null) listing.setMinStayDays(req.minStayDays());
        if (req.floorPlanUrl() != null) listing.setFloorPlanUrl(req.floorPlanUrl());
        if (req.panoramaUrl() != null) listing.setPanoramaUrl(req.panoramaUrl());
        if (req.videoTourUrl() != null) listing.setVideoTourUrl(req.videoTourUrl());
        if (req.neighborhoodPhotoUrls() != null) listing.setNeighborhoodPhotoUrls(req.neighborhoodPhotoUrls());
        if (req.weeklyDiscountPercent() != null) listing.setWeeklyDiscountPercent(req.weeklyDiscountPercent());
        if (req.monthlyDiscountPercent() != null) listing.setMonthlyDiscountPercent(req.monthlyDiscountPercent());
        if (req.cleaningFeePaise() != null) listing.setCleaningFeePaise(req.cleaningFeePaise());
        if (req.visibilityBoostPercent() != null) listing.setVisibilityBoostPercent(validateVisibilityBoost(req.visibilityBoostPercent()));
        // PG/Co-living fields
        if (req.occupancyType() != null) listing.setOccupancyType(req.occupancyType());
        if (req.foodType() != null) listing.setFoodType(req.foodType());
        if (req.gateClosingTime() != null) listing.setGateClosingTime(java.time.LocalTime.parse(req.gateClosingTime()));
        if (req.noticePeriodDays() != null) listing.setNoticePeriodDays(req.noticePeriodDays());
        if (req.securityDepositPaise() != null) listing.setSecurityDepositPaise(req.securityDepositPaise());
        if (req.depositType() != null) listing.setDepositType(req.depositType());
        if (req.depositTerms() != null) listing.setDepositTerms(req.depositTerms());
        if (req.gracePeriodDays() != null) listing.setGracePeriodDays(req.gracePeriodDays());
        if (req.latePenaltyBps() != null) listing.setLatePenaltyBps(req.latePenaltyBps());
        // Insurance
        if (req.insuranceEnabled() != null) listing.setInsuranceEnabled(req.insuranceEnabled());
        if (req.insuranceAmountPaise() != null) listing.setInsuranceAmountPaise(req.insuranceAmountPaise());
        if (req.insuranceType() != null) listing.setInsuranceType(req.insuranceType());
        // Hotel fields
        if (req.hotelChain() != null) listing.setHotelChain(req.hotelChain());
        if (req.frontDesk24h() != null) listing.setFrontDesk24h(req.frontDesk24h());
        if (req.checkoutTime() != null) listing.setCheckoutTime(java.time.LocalTime.parse(req.checkoutTime()));
        if (req.checkinTime() != null) listing.setCheckinTime(java.time.LocalTime.parse(req.checkinTime()));

        return toResponse(listingRepository.save(listing));
    }

    @Cacheable(value = "listings", key = "#id")
    public ListingResponse getListing(UUID id) {
        return toResponse(listingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + id)));
    }

    @Cacheable(value = "hostListings", key = "#hostId")
    public List<ListingResponse> getMyListings(UUID hostId) {
        return listingRepository.findByHostId(hostId).stream()
                .map(this::toResponse).toList();
    }

    public Page<ListingResponse> searchListings(String city, ListingType type,
                                                   Long minPrice, Long maxPrice,
                                                   Pageable pageable) {
        boolean hasFilters = city != null || type != null || minPrice != null || maxPrice != null;
        if (hasFilters) {
            return listingRepository.searchWithFilters(ListingStatus.VERIFIED, city, type,
                            minPrice, maxPrice, pageable)
                    .map(this::toResponse);
        }
        return listingRepository.findByStatus(ListingStatus.VERIFIED, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ListingResponse submitForVerification(UUID listingId, UUID hostId) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT listings can be submitted for verification");
        }

        // Media validation: at least 1 approved photo required
        long photoCount = listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.PHOTO, ModerationStatus.APPROVED);
        if (photoCount == 0) {
            throw new IllegalStateException("At least 1 photo is required before submitting for verification");
        }

        // Must have a primary photo set
        boolean hasPrimary = listingMediaRepository.existsByListingIdAndIsPrimaryTrue(listingId);
        if (!hasPrimary) {
            throw new IllegalStateException("A primary photo must be set before submitting for verification");
        }

        listing.setStatus(ListingStatus.PENDING_VERIFICATION);
        return toResponse(listingRepository.save(listing));
    }

    public Map<String, Object> getVerificationReadiness(UUID listingId, UUID hostId) {
        getListingOwnedBy(listingId, hostId);
        long photoCount = listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.PHOTO, ModerationStatus.APPROVED);
        boolean hasPrimary = listingMediaRepository.existsByListingIdAndIsPrimaryTrue(listingId);
        long videoCount = listingMediaRepository.countByListingIdAndTypeAndModerationStatus(
                listingId, MediaType.VIDEO, ModerationStatus.APPROVED);
        boolean ready = photoCount > 0 && hasPrimary;
        return Map.of(
                "photoCount", photoCount,
                "primaryPhoto", hasPrimary,
                "videoCount", videoCount,
                "ready", ready
        );
    }

    @Transactional
    public ListingResponse pauseListing(UUID listingId, UUID hostId) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() != ListingStatus.VERIFIED) {
            throw new IllegalStateException("Only VERIFIED listings can be paused");
        }
        listing.setStatus(ListingStatus.PAUSED);
        return toResponse(listingRepository.save(listing));
    }

    /**
     * Unpublish a listing back to DRAFT so the host can edit it.
     * Works from VERIFIED, PAUSED, or REJECTED. Must re-verify to go live again.
     */
    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse unpublishToDraft(UUID listingId, UUID hostId) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        ListingStatus current = listing.getStatus();
        if (current == ListingStatus.DRAFT) {
            throw new IllegalStateException("Listing is already in DRAFT");
        }
        if (current == ListingStatus.SUSPENDED) {
            throw new IllegalStateException("Suspended listings can only be managed by admin");
        }
        listing.setStatus(ListingStatus.DRAFT);
        Listing saved = listingRepository.save(listing);
        // Remove from ES — no longer searchable
        kafkaTemplate.send("listing.archived", saved.getId().toString(),
                saved.getId() + "|" + hostId + "|UNPUBLISHED");
        log.info("Listing {} unpublished to DRAFT (was {})", listingId, current);
        return toResponse(saved);
    }

    @Transactional
    public ListingResponse verifyListing(UUID listingId, String notes) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (listing.getStatus() != ListingStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only PENDING_VERIFICATION listings can be verified");
        }
        listing.setStatus(ListingStatus.VERIFIED);
        if (notes != null && !notes.isBlank()) {
            listing.setVerificationNotes(notes);
        }
        Listing saved = listingRepository.save(listing);
        try {
            String payload = String.format("{\"listingId\":\"%s\",\"hostId\":\"%s\"}",
                    saved.getId(), saved.getHostId());
            kafkaTemplate.send("listing.verified", saved.getId().toString(), payload);
            log.info("Listing {} verified and event published", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish listing.verified event for {}: {}", saved.getId(), e.getMessage());
        }
        return toResponse(saved);
    }

    @Transactional
    public ListingResponse rejectListing(UUID listingId, String notes) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (listing.getStatus() != ListingStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only PENDING_VERIFICATION listings can be rejected");
        }
        listing.setStatus(ListingStatus.REJECTED);
        listing.setVerificationNotes(notes);
        return toResponse(listingRepository.save(listing));
    }

    public List<ListingResponse> getListingsByStatus(ListingStatus status) {
        return listingRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public void deleteListing(UUID listingId, UUID hostId) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT listings can be deleted");
        }
        listingRepository.delete(listing);
    }

    /**
     * Host self-archives their listing. Reversible — restores to DRAFT (must re-verify).
     * Works from any status except SUSPENDED (admin-controlled).
     */
    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse archiveListing(UUID listingId, UUID hostId, ArchiveReason reason, String note) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() == ListingStatus.SUSPENDED) {
            throw new IllegalStateException("Suspended listings can only be managed by admin");
        }
        if (listing.getStatus() == ListingStatus.ARCHIVED) {
            throw new IllegalStateException("Listing is already archived");
        }

        listing.setPreviousStatus(listing.getStatus().name());
        listing.setStatus(ListingStatus.ARCHIVED);
        listing.setArchiveReason(reason != null ? reason : ArchiveReason.HOST_REQUEST);
        listing.setArchiveNote(note);
        listing.setArchivedBy(hostId);
        listing.setArchivedAt(OffsetDateTime.now());

        Listing saved = listingRepository.save(listing);
        kafkaTemplate.send("listing.archived", saved.getId().toString(),
                saved.getId() + "|" + hostId + "|" + saved.getArchiveReason().name());
        log.info("Listing {} archived by host (reason: {})", listingId, reason);
        return toResponse(saved);
    }

    /**
     * Admin suspends a listing (fraud, policy violation, duplicate).
     * Host CANNOT restore — only admin can.
     */
    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse suspendListing(UUID listingId, UUID adminId, ArchiveReason reason, String note) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        listing.setPreviousStatus(listing.getStatus().name());
        listing.setStatus(ListingStatus.SUSPENDED);
        listing.setArchiveReason(reason);
        listing.setArchiveNote(note);
        listing.setArchivedBy(adminId);
        listing.setArchivedAt(OffsetDateTime.now());

        Listing saved = listingRepository.save(listing);
        kafkaTemplate.send("listing.suspended", saved.getId().toString(),
                saved.getId() + "|" + adminId + "|" + reason.name());
        log.info("Listing {} SUSPENDED by admin {} (reason: {})", listingId, adminId, reason);
        return toResponse(saved);
    }

    /**
     * Restore an archived listing. Goes back to DRAFT (host must re-verify).
     * Host can restore ARCHIVED; only admin can restore SUSPENDED.
     */
    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse restoreListing(UUID listingId, UUID userId, boolean isAdmin) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        if (listing.getStatus() == ListingStatus.SUSPENDED && !isAdmin) {
            throw new IllegalStateException("Suspended listings can only be restored by admin");
        }
        if (listing.getStatus() == ListingStatus.ARCHIVED && !isAdmin) {
            // Host restore — verify ownership
            if (!listing.getHostId().equals(userId)) {
                throw new IllegalStateException("Not the owner of this listing");
            }
        }
        if (listing.getStatus() != ListingStatus.ARCHIVED && listing.getStatus() != ListingStatus.SUSPENDED) {
            throw new IllegalStateException("Only ARCHIVED or SUSPENDED listings can be restored");
        }

        listing.setStatus(ListingStatus.DRAFT);
        listing.setArchiveReason(null);
        listing.setArchiveNote(null);
        listing.setArchivedBy(null);
        listing.setArchivedAt(null);
        listing.setPreviousStatus(null);

        Listing saved = listingRepository.save(listing);
        log.info("Listing {} restored to DRAFT by {}", listingId, userId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMedia(UUID listingId, UUID mediaId, UUID userId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));
        if (!listing.getHostId().equals(userId)) {
            throw new IllegalStateException("Not the owner");
        }
        ListingMedia media = listingMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NoSuchElementException("Media not found"));
        if (!media.getListingId().equals(listingId)) {
            throw new IllegalStateException("Media doesn't belong to this listing");
        }
        listingMediaRepository.delete(media);
    }

    @Transactional
    public void reorderMedia(UUID listingId, UUID userId, List<UUID> mediaIds) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));
        if (!listing.getHostId().equals(userId)) {
            throw new IllegalStateException("Not the owner");
        }
        for (int idx = 0; idx < mediaIds.size(); idx++) {
            int sortOrder = idx;
            listingMediaRepository.findById(mediaIds.get(idx)).ifPresent(media -> {
                if (media.getListingId().equals(listingId)) {
                    media.setSortOrder(sortOrder);
                    listingMediaRepository.save(media);
                }
            });
        }
    }

    @Transactional
    public void updateMediaCategory(UUID listingId, UUID mediaId, UUID userId, String category) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));
        if (!listing.getHostId().equals(userId)) {
            throw new IllegalStateException("Not the owner");
        }
        ListingMedia media = listingMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NoSuchElementException("Media not found"));
        media.setCategory(category);
        listingMediaRepository.save(media);
    }

    @CacheEvict(value = {"listings", "hostListings"}, allEntries = true)
    @Transactional
    public ListingResponse toggleAashray(UUID listingId, UUID hostId, Map<String, Object> body) {
        Listing listing = getListingOwnedBy(listingId, hostId);
        if (listing.getStatus() != ListingStatus.VERIFIED) {
            throw new IllegalStateException("Only VERIFIED listings can be toggled for Aashray");
        }

        Boolean aashrayReady = body.get("aashrayReady") instanceof Boolean
                ? (Boolean) body.get("aashrayReady") : false;
        listing.setAashrayReady(aashrayReady);

        if (Boolean.TRUE.equals(aashrayReady)) {
            // Validate and set discount percent (0-100, default 10)
            int discount = 10;
            if (body.get("aashrayDiscountPercent") instanceof Number) {
                discount = ((Number) body.get("aashrayDiscountPercent")).intValue();
                if (discount < 0 || discount > 100) {
                    throw new IllegalArgumentException("aashrayDiscountPercent must be between 0 and 100");
                }
            }
            listing.setAashrayDiscountPercent(discount);

            // Set long-term monthly price if provided
            if (body.get("longTermMonthlyPaise") instanceof Number) {
                listing.setLongTermMonthlyPaise(((Number) body.get("longTermMonthlyPaise")).longValue());
            }

            // Set min stay days (min 30, default 30)
            int minStay = 30;
            if (body.get("minStayDays") instanceof Number) {
                minStay = ((Number) body.get("minStayDays")).intValue();
                if (minStay < 30) {
                    throw new IllegalArgumentException("minStayDays must be at least 30 for Aashray listings");
                }
            }
            listing.setMinStayDays(minStay);
        }

        Listing saved = listingRepository.save(listing);
        log.info("Listing {} aashrayReady toggled to {} by host {}", listingId, aashrayReady, hostId);
        return toResponse(saved);
    }

    private java.time.LocalTime parseTime(String time, String fallback) {
        try {
            String val = time != null ? time : fallback;
            return val != null ? java.time.LocalTime.parse(val) : null;
        } catch (Exception e) {
            return fallback != null ? java.time.LocalTime.parse(fallback) : null;
        }
    }

    private Listing getListingOwnedBy(UUID listingId, UUID hostId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Listing does not belong to this host");
        }
        return listing;
    }

    @Transactional
    public int geocodeAllMissing() {
        List<Listing> missing = listingRepository.findByLatAndLng(BigDecimal.ZERO, BigDecimal.ZERO);
        int updated = 0;
        for (Listing listing : missing) {
            BigDecimal[] coords = geocodingService.geocode(
                    listing.getPincode(), listing.getCity(), listing.getState());
            if (coords != null) {
                listing.setLat(coords[0]);
                listing.setLng(coords[1]);
                listingRepository.save(listing);
                updated++;
            }
            // Nominatim rate limit: max 1 request/second
            try { Thread.sleep(1100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        log.info("Batch geocoded {}/{} listings", updated, missing.size());
        return updated;
    }

    /**
     * One-shot backfill on startup: any existing PG/COLIVING listing without a
     * RoomType row gets a default one. Safe to run repeatedly — only acts on
     * listings whose room_types collection is empty.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingPgRoomTypes() {
        try {
            List<Listing> pgListings = listingRepository.findAll().stream()
                    .filter(l -> l.getType() == ListingType.PG || l.getType() == ListingType.COLIVING)
                    .toList();
            int seeded = 0;
            for (Listing l : pgListings) {
                if (roomTypeRepository.findByListingIdOrderBySortOrder(l.getId()).isEmpty()) {
                    seedDefaultPgRoomType(l);
                    seeded++;
                }
            }
            if (seeded > 0) {
                log.info("Backfilled default RoomType for {} PG/COLIVING listings", seeded);
            }
        } catch (Exception e) {
            log.warn("PG RoomType backfill failed: {}", e.getMessage());
        }
    }

    /**
     * Create a sensible default RoomType for newly created PG/COLIVING listings so
     * the host & admin room board show occupancy immediately. The host can edit
     * sharingType, count, price, etc. via the Room Types tab.
     */
    private void seedDefaultPgRoomType(Listing listing) {
        try {
            int rooms = listing.getTotalRooms() != null && listing.getTotalRooms() > 0
                    ? listing.getTotalRooms() : 1;
            RoomType defaultRoom = RoomType.builder()
                    .listingId(listing.getId())
                    .name("Standard Room")
                    .description("Default room — please update sharing type, beds, and price.")
                    .count(rooms)
                    .basePricePaise(listing.getBasePricePaise())
                    .maxGuests(1)
                    .bedCount(1)
                    .stayMode(StayMode.MONTHLY)
                    .sharingType(SharingType.PRIVATE)
                    .totalBeds(rooms) // PRIVATE → 1 bed per room
                    .occupiedBeds(0)
                    .securityDepositPaise(listing.getSecurityDepositPaise())
                    .depositType(listing.getDepositType() != null ? listing.getDepositType() : "REFUNDABLE")
                    .sortOrder(0)
                    .build();
            roomTypeRepository.save(defaultRoom);
            log.info("Seeded default RoomType for PG listing {} ({} rooms)", listing.getId(), rooms);
        } catch (Exception e) {
            // Non-fatal — host can add room types manually if this fails
            log.warn("Failed to seed default RoomType for listing {}: {}", listing.getId(), e.getMessage());
        }
    }

    /**
     * Create room types from the wizard-provided list instead of the default seed.
     */
    private void createWizardRoomTypes(Listing listing, List<RoomTypeRequest> roomTypes) {
        try {
            int order = 0;
            for (var rt : roomTypes) {
                SharingType sharing = rt.sharingType() != null
                        ? SharingType.valueOf(rt.sharingType()) : SharingType.PRIVATE;
                int beds = deriveTotalBeds(sharing, rt.count() != null ? rt.count() : 1);
                RoomType room = RoomType.builder()
                        .listingId(listing.getId())
                        .name(rt.name())
                        .description(rt.description())
                        .count(rt.count() != null ? rt.count() : 1)
                        .basePricePaise(rt.basePricePaise())
                        .maxGuests(rt.maxGuests() != null ? rt.maxGuests() : 1)
                        .bedCount(rt.bedCount() != null ? rt.bedCount() : 1)
                        .stayMode(StayMode.MONTHLY)
                        .sharingType(sharing)
                        .roomVariant(rt.roomVariant() != null ? RoomVariant.valueOf(rt.roomVariant()) : null)
                        .totalBeds(beds)
                        .occupiedBeds(0)
                        .securityDepositPaise(rt.securityDepositPaise() != null
                                ? rt.securityDepositPaise() : listing.getSecurityDepositPaise())
                        .depositType(rt.depositType() != null ? rt.depositType()
                                : (listing.getDepositType() != null ? listing.getDepositType() : "REFUNDABLE"))
                        .sortOrder(order++)
                        .build();
                roomTypeRepository.save(room);
            }
            log.info("Created {} wizard-provided room types for PG listing {}", roomTypes.size(), listing.getId());
        } catch (Exception e) {
            log.warn("Failed to create wizard room types for listing {}: {} — falling back to default seed",
                    listing.getId(), e.getMessage());
            seedDefaultPgRoomType(listing);
        }
    }

    /**
     * Derive total beds from sharing type × room count.
     */
    private int deriveTotalBeds(SharingType sharing, int roomCount) {
        int bedsPerRoom = switch (sharing) {
            case PRIVATE -> 1;
            case TWO_SHARING -> 2;
            case THREE_SHARING -> 3;
            case FOUR_SHARING -> 4;
            case DORMITORY -> 6; // sensible default for dorm
        };
        return bedsPerRoom * roomCount;
    }

    private ListingResponse toResponse(Listing l) {
        var primaryMedia = listingMediaRepository.findFirstByListingIdAndIsPrimaryTrue(l.getId());
        String primaryPhotoUrl = primaryMedia != null ? primaryMedia.getCdnUrl() : null;

        // Medical tourism enrichment — batch hospital lookup to avoid N+1
        List<String> hospitalNames = List.of();
        List<String> medicalSpecialties = List.of();
        if (Boolean.TRUE.equals(l.getMedicalStay())) {
            List<MedicalStayPackage> medPkgs = medicalStayPackageRepository.findByListingId(l.getId());
            List<UUID> hospitalIds = medPkgs.stream()
                    .map(MedicalStayPackage::getHospitalId)
                    .distinct().toList();
            List<HospitalPartner> hospitals = hospitalPartnerRepository.findAllById(hospitalIds);
            hospitalNames = hospitals.stream()
                    .map(HospitalPartner::getName).distinct().toList();
            medicalSpecialties = hospitals.stream()
                    .filter(h -> h.getSpecialties() != null)
                    .flatMap(h -> java.util.Arrays.stream(h.getSpecialties().split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct().toList();
        }

        return new ListingResponse(
                l.getId(), l.getHostId(),
                l.getTitle(), l.getDescription(),
                l.getType(), l.getCommercialCategory(),
                l.getAddressLine1(), l.getAddressLine2(),
                l.getCity(), l.getState(), l.getPincode(),
                l.getLat(), l.getLng(),
                l.getMaxGuests(), l.getBedrooms(), l.getBathrooms(),
                l.getTotalRooms(),
                l.getAmenities(),
                l.getBasePricePaise(), l.getPricingUnit(),
                l.getMinBookingHours(), l.getInstantBook(),
                l.getStatus(), l.getVerificationNotes(),
                l.getGstApplicable(), l.getGstin(),
                l.getPetFriendly(), l.getMaxPets(),
                l.getAvgRating(), l.getReviewCount(),
                primaryPhotoUrl,
                l.getStarRating(), l.getCancellationPolicy(), l.getMealPlan(),
                l.getBedTypes(), l.getAccessibilityFeatures(),
                l.getFreeCancellation(), l.getNoPrepayment(),
                l.getCheckInFrom() != null ? l.getCheckInFrom().toString() : null,
                l.getCheckInUntil() != null ? l.getCheckInUntil().toString() : null,
                l.getCheckOutFrom() != null ? l.getCheckOutFrom().toString() : null,
                l.getCheckOutUntil() != null ? l.getCheckOutUntil().toString() : null,
                l.getChildrenAllowed(), l.getParkingType(), l.getBreakfastIncluded(),
                l.getAreaSqft(),
                l.getOperatingHoursFrom() != null ? l.getOperatingHoursFrom().toString() : null,
                l.getOperatingHoursUntil() != null ? l.getOperatingHoursUntil().toString() : null,
                l.getAashrayReady(), l.getAashrayDiscountPercent(),
                l.getLongTermMonthlyPaise(), l.getMinStayDays(),
                l.getFloorPlanUrl(), l.getPanoramaUrl(), l.getVideoTourUrl(),
                l.getNeighborhoodPhotoUrls(),
                l.getMedicalStay(), hospitalNames, medicalSpecialties,
                l.getWeeklyDiscountPercent(), l.getMonthlyDiscountPercent(),
                l.getCleaningFeePaise(),
                l.getVisibilityBoostPercent(),
                false, // preferredPartner — resolved at search/reindex time from subscription data
                // PG/Co-living fields
                l.getOccupancyType(),
                l.getFoodType(),
                l.getGateClosingTime() != null ? l.getGateClosingTime().toString() : null,
                l.getNoticePeriodDays(),
                l.getSecurityDepositPaise(),
                l.getDepositType(),
                l.getDepositTerms(),
                l.getGracePeriodDays(),
                l.getLatePenaltyBps(),
                // Insurance
                l.getInsuranceEnabled(),
                l.getInsuranceAmountPaise(),
                l.getInsuranceType(),
                // Hotel fields
                l.getHotelChain(),
                l.getFrontDesk24h(),
                l.getCheckoutTime() != null ? l.getCheckoutTime().toString() : null,
                l.getCheckinTime() != null ? l.getCheckinTime().toString() : null,
                // Hotel enhancements
                l.getCoupleFriendly(),
                l.getPropertyHighlights(),
                l.getEarlyBirdDiscountPercent(),
                l.getEarlyBirdDaysBefore(),
                l.getZeroPaymentBooking(),
                l.getLocationHighlight(),
                l.getCreatedAt(), l.getUpdatedAt()
        );
    }

    private Integer validateVisibilityBoost(Integer value) {
        if (value == null) return 0;
        if (value != 0 && value != 3 && value != 5) {
            throw new IllegalArgumentException("visibilityBoostPercent must be 0, 3, or 5");
        }
        return value;
    }
}
