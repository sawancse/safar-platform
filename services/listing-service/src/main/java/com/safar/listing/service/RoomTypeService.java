package com.safar.listing.service;

import com.safar.listing.dto.RoomTypeInclusionResponse;
import com.safar.listing.dto.RoomTypeRequest;
import com.safar.listing.dto.RoomTypeResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.RoomType;
import com.safar.listing.entity.RoomTypeAvailability;
import com.safar.listing.entity.RoomTypeInclusion;
import com.safar.listing.entity.enums.SharingType;
import com.safar.listing.entity.enums.StayMode;
import com.safar.listing.entity.enums.RoomVariant;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.RoomTypeAvailabilityRepository;
import com.safar.listing.repository.RoomTypeInclusionRepository;
import com.safar.listing.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepo;
    private final RoomTypeAvailabilityRepository availabilityRepo;
    private final RoomTypeInclusionRepository inclusionRepo;
    private final ListingRepository listingRepo;

    @Transactional
    public RoomTypeResponse createRoomType(UUID listingId, UUID hostId, RoomTypeRequest req) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        int nextSort = roomTypeRepo.findByListingIdOrderBySortOrder(listingId).size();

        validateMaxGuestsAgainstSharing(req.sharingType(), req.maxGuests());

        RoomType roomType = RoomType.builder()
                .listingId(listingId)
                .name(req.name())
                .description(req.description())
                .count(req.count())
                .basePricePaise(req.basePricePaise())
                .maxGuests(req.maxGuests())
                .bedType(req.bedType())
                .bedCount(req.bedCount() != null ? req.bedCount() : 1)
                .areaSqft(req.areaSqft())
                .amenities(req.amenities() != null ? String.join(",", req.amenities()) : null)
                .stayMode(req.stayMode() != null ? StayMode.valueOf(req.stayMode()) : StayMode.NIGHTLY)
                .sharingType(req.sharingType() != null ? SharingType.valueOf(req.sharingType()) : null)
                .roomVariant(req.roomVariant() != null ? RoomVariant.valueOf(req.roomVariant()) : null)
                .totalBeds(req.sharingType() != null ? computeTotalBeds(req.sharingType(), req.count()) : null)
                .securityDepositPaise(req.securityDepositPaise())
                .depositType(req.depositType() != null ? req.depositType() : "REFUNDABLE")
                .sortOrder(nextSort)
                .primaryPhotoUrl(req.primaryPhotoUrl())
                .photoUrls(req.photoUrls() != null ? String.join(",", req.photoUrls()) : null)
                .build();

        RoomType saved = roomTypeRepo.save(roomType);

        // Availability is created on-demand when first booking checks it.
        // computeMinAvailable() defaults to roomType.getCount() when no records exist.
        // No need to pre-initialize 90 days of records (was causing timeout).

        log.info("Created room type '{}' for listing {}", saved.getName(), listingId);
        return toResponse(saved, null);
    }

    @Transactional
    public RoomTypeResponse updateRoomType(UUID listingId, UUID roomTypeId, UUID hostId, RoomTypeRequest req) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));
        if (!roomType.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Room type does not belong to this listing");
        }

        String effectiveSharing = req.sharingType() != null
                ? req.sharingType()
                : (roomType.getSharingType() != null ? roomType.getSharingType().name() : null);
        validateMaxGuestsAgainstSharing(effectiveSharing, req.maxGuests());

        roomType.setName(req.name());
        roomType.setDescription(req.description());
        roomType.setCount(req.count());
        roomType.setBasePricePaise(req.basePricePaise());
        roomType.setMaxGuests(req.maxGuests());
        roomType.setBedType(req.bedType());
        roomType.setBedCount(req.bedCount() != null ? req.bedCount() : 1);
        roomType.setAreaSqft(req.areaSqft());
        roomType.setAmenities(req.amenities() != null ? String.join(",", req.amenities()) : null);
        if (req.stayMode() != null) roomType.setStayMode(StayMode.valueOf(req.stayMode()));
        if (req.sharingType() != null) {
            roomType.setSharingType(SharingType.valueOf(req.sharingType()));
            roomType.setTotalBeds(computeTotalBeds(req.sharingType(), req.count()));
        }
        if (req.roomVariant() != null) roomType.setRoomVariant(RoomVariant.valueOf(req.roomVariant()));
        roomType.setSecurityDepositPaise(req.securityDepositPaise());
        if (req.depositType() != null) roomType.setDepositType(req.depositType());
        if (req.primaryPhotoUrl() != null) roomType.setPrimaryPhotoUrl(req.primaryPhotoUrl());
        if (req.photoUrls() != null) roomType.setPhotoUrls(String.join(",", req.photoUrls()));

        RoomType updated = roomTypeRepo.save(roomType);
        log.info("Updated room type '{}' for listing {}", updated.getName(), listingId);
        return toResponse(updated, null);
    }

    @Transactional
    public void deleteRoomType(UUID listingId, UUID roomTypeId, UUID hostId) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));
        if (!roomType.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Room type does not belong to this listing");
        }

        roomTypeRepo.delete(roomType);
        log.info("Deleted room type {} from listing {}", roomTypeId, listingId);
    }

    public List<RoomTypeResponse> getRoomTypes(UUID listingId) {
        return roomTypeRepo.findByListingIdOrderBySortOrder(listingId).stream()
                .map(rt -> toResponse(rt, roomsAvailableNow(rt)))
                .toList();
    }

    public List<RoomTypeResponse> getAvailableRoomTypes(UUID listingId, LocalDate checkIn, LocalDate checkOut) {
        List<RoomType> roomTypes = roomTypeRepo.findByListingIdOrderBySortOrder(listingId);
        List<RoomTypeResponse> result = new ArrayList<>();

        for (RoomType rt : roomTypes) {
            int minAvailable = computeMinAvailable(rt.getId(), checkIn, checkOut, rt.getCount());
            // Cap by current room-level occupancy (tenancies never write to
            // RoomTypeAvailability, so the date table can overstate PG availability).
            int occupancyCap = roomsAvailableNow(rt);
            int effective = Math.min(minAvailable, occupancyCap);
            result.add(toResponse(rt, effective));
        }

        return result;
    }

    /**
     * Rooms currently bookable = count − rooms fully or partially occupied.
     * Uses `occupiedBeds` aggregate which is maintained by tenancy events and
     * (as of 2026-04-11) booking create/cancel flows. Pessimistic: a partially
     * occupied multi-sharing room counts as unavailable for whole-room booking.
     */
    private int roomsAvailableNow(RoomType rt) {
        int count = rt.getCount() != null ? rt.getCount() : 0;
        int occupiedBeds = rt.getOccupiedBeds() != null ? rt.getOccupiedBeds() : 0;
        int bedsPerRoom = effectiveBedsPerRoom(rt);
        if (bedsPerRoom <= 0) bedsPerRoom = 1;
        if (isSharedType(rt)) {
            // Shared PG: a partially-occupied room is still bookable (another
            // tenant can take a free bed). Use floor to keep rooms-with-free-bed
            // in the available pool.
            int roomsFullyOccupied = occupiedBeds / bedsPerRoom;
            return Math.max(0, count - roomsFullyOccupied);
        }
        // PRIVATE: any occupied bed in a room makes the whole room unavailable.
        int roomsOccupied = (int) Math.ceil((double) occupiedBeds / bedsPerRoom);
        return Math.max(0, count - roomsOccupied);
    }

    @Transactional
    public void decrementAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        LocalDate current = from;
        while (!current.isAfter(to.minusDays(1))) {
            LocalDate date = current;
            RoomTypeAvailability avail = availabilityRepo.findByRoomTypeIdAndDate(roomTypeId, date)
                    .orElseGet(() -> RoomTypeAvailability.builder()
                            .roomTypeId(roomTypeId)
                            .date(date)
                            .availableCount(roomType.getCount())
                            .build());

            if (avail.getAvailableCount() < count) {
                throw new IllegalStateException(
                        "Not enough rooms available on " + date + ". Available: " + avail.getAvailableCount() + ", requested: " + count);
            }

            avail.setAvailableCount(avail.getAvailableCount() - count);
            availabilityRepo.save(avail);
            current = current.plusDays(1);
        }

        log.info("Decremented availability for room type {} from {} to {} by {}", roomTypeId, from, to, count);
    }

    @Transactional
    public void incrementAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        LocalDate current = from;
        while (!current.isAfter(to.minusDays(1))) {
            LocalDate date = current;
            RoomTypeAvailability avail = availabilityRepo.findByRoomTypeIdAndDate(roomTypeId, date)
                    .orElseGet(() -> RoomTypeAvailability.builder()
                            .roomTypeId(roomTypeId)
                            .date(date)
                            .availableCount(roomType.getCount())
                            .build());

            avail.setAvailableCount(avail.getAvailableCount() + count);
            availabilityRepo.save(avail);
            current = current.plusDays(1);
        }

        log.info("Incremented availability for room type {} from {} to {} by {}", roomTypeId, from, to, count);
    }

    public RoomTypeResponse getRoomTypeById(UUID roomTypeId) {
        RoomType rt = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));
        return toResponse(rt, null);
    }

    @Transactional
    public RoomTypeResponse updatePhotos(UUID listingId, UUID roomTypeId, UUID hostId,
                                          String primaryPhotoUrl, List<String> photoUrls) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }
        RoomType rt = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found"));
        if (!rt.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Room type does not belong to this listing");
        }

        if (primaryPhotoUrl != null) rt.setPrimaryPhotoUrl(primaryPhotoUrl);
        if (photoUrls != null) {
            if (photoUrls.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 photos per room type");
            }
            rt.setPhotoUrls(String.join(",", photoUrls));
        }

        RoomType saved = roomTypeRepo.save(rt);
        log.info("Updated photos for room type '{}' (primary={}, gallery={})",
                saved.getName(), primaryPhotoUrl != null, photoUrls != null ? photoUrls.size() : 0);
        return toResponse(saved, null);
    }

    public int computeMinAvailablePublic(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int defaultCount) {
        return computeMinAvailable(roomTypeId, checkIn, checkOut, defaultCount);
    }

    /**
     * Reset room-type availability to full capacity for a date range.
     * Used when bookings are cancelled or availability was incorrectly decremented.
     */
    @Transactional
    public void resetAvailability(UUID roomTypeId, LocalDate from, LocalDate to) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        LocalDate current = from;
        while (!current.isAfter(to.minusDays(1))) {
            LocalDate date = current;
            RoomTypeAvailability avail = availabilityRepo.findByRoomTypeIdAndDate(roomTypeId, date)
                    .orElse(null);
            if (avail != null) {
                avail.setAvailableCount(roomType.getCount());
                availabilityRepo.save(avail);
            }
            current = current.plusDays(1);
        }
        log.info("Reset availability for room type {} from {} to {} (capacity={})",
                roomTypeId, from, to, roomType.getCount());
    }

    /**
     * Increment occupiedBeds when a new PG tenant moves in.
     * bedsToOccupy is calculated from sharingType: PRIVATE = all beds in one room, shared = 1 bed.
     */
    @Transactional
    public void incrementOccupancy(UUID roomTypeId, String sharingType) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        int beds = bedsForSharingType(sharingType, roomType);
        int current = roomType.getOccupiedBeds() != null ? roomType.getOccupiedBeds() : 0;
        int total = roomType.getTotalBeds() != null ? roomType.getTotalBeds() : roomType.getCount();

        if (current + beds > total) {
            throw new IllegalStateException("Cannot occupy " + beds + " bed(s) — only " + (total - current) + " available in room type " + roomTypeId);
        }

        roomType.setOccupiedBeds(current + beds);
        roomTypeRepo.save(roomType);
        log.info("Occupancy incremented for room type {} by {} beds (now {}/{})",
                roomTypeId, beds, roomType.getOccupiedBeds(), total);
    }

    /**
     * Decrement occupiedBeds when a PG tenant vacates.
     * Also restores date-based availability for the next 12 months from moveOutDate.
     */
    @Transactional
    public void decrementOccupancy(UUID roomTypeId, String sharingType, LocalDate moveOutDate) {
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        int beds = bedsForSharingType(sharingType, roomType);
        int current = roomType.getOccupiedBeds() != null ? roomType.getOccupiedBeds() : 0;

        roomType.setOccupiedBeds(Math.max(0, current - beds));
        roomTypeRepo.save(roomType);
        log.info("Occupancy decremented for room type {} by {} beds (now {}/{})",
                roomTypeId, beds, roomType.getOccupiedBeds(),
                roomType.getTotalBeds() != null ? roomType.getTotalBeds() : roomType.getCount());

        // Restore date-based availability for 12 months from moveOutDate
        if (moveOutDate != null) {
            LocalDate from = moveOutDate.isAfter(LocalDate.now()) ? moveOutDate : LocalDate.now();
            LocalDate to = from.plusMonths(12);
            incrementAvailability(roomTypeId, from, to, 1);
            log.info("Restored date availability for room type {} from {} to {}", roomTypeId, from, to);
        }
    }

    /**
     * Booking-style occupancy. Semantics depend on sharingType:
     *
     *   PRIVATE → `units` = number of rooms. Each consumes bedCount beds
     *             (whole-room booking, e.g. a couple reserving a Deluxe Double).
     *   SHARED  → `units` = number of beds (guests). Each consumes exactly
     *             1 bed (e.g. a PG tenant taking one bed in a TWO_SHARING room;
     *             the other bed remains available).
     *
     * Booking-service always passes `sel.count()` which maps to rooms for
     * whole-room listings and to guests for bed-level PG listings.
     */
    @Transactional
    public void incrementOccupancyForBooking(UUID roomTypeId, int units) {
        if (units <= 0) return;
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        int delta = bedsConsumedByBookingUnits(roomType, units);
        int current = roomType.getOccupiedBeds() != null ? roomType.getOccupiedBeds() : 0;
        int total = effectiveTotalBeds(roomType);

        if (current + delta > total) {
            throw new IllegalStateException("Cannot occupy " + delta + " beds — only "
                    + (total - current) + " available in room type " + roomTypeId);
        }
        roomType.setOccupiedBeds(current + delta);
        roomTypeRepo.save(roomType);
        log.info("Booking occupancy +{} beds ({} {} on {}) on room type {} ({}/{})",
                delta, units,
                isSharedType(roomType) ? "guests" : "rooms",
                roomType.getSharingType(), roomTypeId, roomType.getOccupiedBeds(), total);
    }

    @Transactional
    public void decrementOccupancyForBooking(UUID roomTypeId, int units) {
        if (units <= 0) return;
        RoomType roomType = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));

        int delta = bedsConsumedByBookingUnits(roomType, units);
        int current = roomType.getOccupiedBeds() != null ? roomType.getOccupiedBeds() : 0;
        roomType.setOccupiedBeds(Math.max(0, current - delta));
        roomTypeRepo.save(roomType);
        log.info("Booking occupancy -{} beds ({} {}) on room type {} (now {})",
                delta, units, isSharedType(roomType) ? "guests" : "rooms",
                roomTypeId, roomType.getOccupiedBeds());
    }

    /**
     * Translate a booking's count value into physical beds consumed.
     * Shared sharingType → 1 bed per unit. PRIVATE/null → full bedCount per unit.
     */
    private int bedsConsumedByBookingUnits(RoomType rt, int units) {
        return isSharedType(rt) ? units : units * effectiveBedsPerRoom(rt);
    }

    private boolean isSharedType(RoomType rt) {
        if (rt.getSharingType() == null) return false;
        return rt.getSharingType() != com.safar.listing.entity.enums.SharingType.PRIVATE;
    }

    /**
     * Physical beds per room: prefer `bedCount` (a "Deluxe Double" PRIVATE has
     * bedCount=2), fall back to the sharingType default for legacy rows.
     */
    private int effectiveBedsPerRoom(RoomType rt) {
        if (rt.getBedCount() != null && rt.getBedCount() > 0) return rt.getBedCount();
        return rt.getSharingType() != null ? computeTotalBeds(rt.getSharingType().name(), 1) : 1;
    }

    private int effectiveTotalBeds(RoomType rt) {
        int count = rt.getCount() != null ? rt.getCount() : 0;
        return count * effectiveBedsPerRoom(rt);
    }

    /**
     * Force-set occupiedBeds on a room type. Used by the cross-service reconcile
     * flow in InternalListingController to fix drift after semantic changes.
     */
    @Transactional
    public void setOccupiedBeds(UUID roomTypeId, int beds) {
        RoomType rt = roomTypeRepo.findById(roomTypeId)
                .orElseThrow(() -> new NoSuchElementException("Room type not found: " + roomTypeId));
        int total = effectiveTotalBeds(rt);
        int clamped = Math.max(0, Math.min(beds, total));
        int prev = rt.getOccupiedBeds() != null ? rt.getOccupiedBeds() : 0;
        rt.setOccupiedBeds(clamped);
        roomTypeRepo.save(rt);
        log.info("Occupancy reconciled on room type {} ({}): {} → {}",
                rt.getName(), roomTypeId, prev, clamped);
    }

    /**
     * Startup backfill: ensure `total_beds` on every room type matches the new
     * bedCount-aware math. Cheap idempotent scan. Also clamps `occupiedBeds` so
     * stale aggregates can't exceed the recomputed capacity.
     */
    @org.springframework.context.event.EventListener(
            org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void backfillTotalBeds() {
        List<RoomType> all = roomTypeRepo.findAll();
        int fixed = 0, clamped = 0;
        for (RoomType rt : all) {
            int expected = effectiveTotalBeds(rt);
            boolean changed = false;
            if (rt.getTotalBeds() == null || rt.getTotalBeds() != expected) {
                rt.setTotalBeds(expected);
                changed = true;
                fixed++;
            }
            int occ = rt.getOccupiedBeds() != null ? rt.getOccupiedBeds() : 0;
            if (occ > expected) {
                rt.setOccupiedBeds(expected);
                changed = true;
                clamped++;
            }
            if (changed) roomTypeRepo.save(rt);
        }
        if (fixed > 0 || clamped > 0) {
            log.info("RoomType backfill: totalBeds fixed={}, occupiedBeds clamped={}", fixed, clamped);
        }
    }

    /**
     * Calculate how many beds one tenancy occupies based on sharing type.
     * PRIVATE = entire room (all beds), shared = 1 bed.
     */
    private int bedsForSharingType(String sharingType, RoomType roomType) {
        if (sharingType == null || "PRIVATE".equals(sharingType)) {
            // Private tenant occupies all beds in one room unit
            return roomType.getSharingType() != null ? computeTotalBeds(roomType.getSharingType().name(), 1) : 1;
        }
        // Shared occupant takes 1 bed
        return 1;
    }

    // ── Private helpers ─────────────────────────────────────────

    private void initializeAvailability(UUID roomTypeId, int count, int days) {
        LocalDate today = LocalDate.now();
        List<RoomTypeAvailability> batch = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            batch.add(RoomTypeAvailability.builder()
                    .roomTypeId(roomTypeId)
                    .date(today.plusDays(i))
                    .availableCount(count)
                    .minStayNights(1)
                    .build());
        }
        availabilityRepo.saveAll(batch);
    }

    private int computeMinAvailable(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int defaultCount) {
        List<RoomTypeAvailability> avails = availabilityRepo
                .findByRoomTypeIdAndDateBetween(roomTypeId, checkIn, checkOut.minusDays(1));

        // Build a map of date -> available count
        Map<LocalDate, Integer> availMap = avails.stream()
                .collect(Collectors.toMap(RoomTypeAvailability::getDate, RoomTypeAvailability::getAvailableCount));

        int min = defaultCount;
        LocalDate current = checkIn;
        while (!current.isAfter(checkOut.minusDays(1))) {
            int available = availMap.getOrDefault(current, defaultCount);
            min = Math.min(min, available);
            current = current.plusDays(1);
        }

        return min;
    }

    private RoomTypeResponse toResponse(RoomType rt, Integer availableCount) {
        List<String> amenitiesList = rt.getAmenities() != null && !rt.getAmenities().isBlank()
                ? Arrays.asList(rt.getAmenities().split(","))
                : List.of();

        List<RoomTypeInclusionResponse> inclusions = inclusionRepo
                .findByRoomTypeIdAndIsActiveTrueOrderBySortOrder(rt.getId()).stream()
                .map(i -> new RoomTypeInclusionResponse(
                        i.getId(), i.getRoomTypeId(), i.getCategory().name(),
                        i.getName(), i.getDescription(), i.getInclusionMode().name(),
                        i.getChargePaise(), i.getChargeType().name(),
                        i.getDiscountPercent(), i.getTerms(),
                        i.getIsHighlight(), i.getSortOrder(), i.getIsActive()))
                .toList();

        return new RoomTypeResponse(
                rt.getId(),
                rt.getListingId(),
                rt.getName(),
                rt.getDescription(),
                rt.getCount(),
                rt.getBasePricePaise(),
                rt.getMaxGuests(),
                rt.getBedType(),
                rt.getBedCount(),
                rt.getAreaSqft(),
                amenitiesList,
                rt.getSortOrder(),
                availableCount,
                rt.getStayMode() != null ? rt.getStayMode().name() : "NIGHTLY",
                rt.getSharingType() != null ? rt.getSharingType().name() : null,
                rt.getRoomVariant() != null ? rt.getRoomVariant().name() : null,
                rt.getTotalBeds(),
                rt.getOccupiedBeds(),
                rt.getSecurityDepositPaise(),
                rt.getDepositType(),
                rt.getPrimaryPhotoUrl(),
                rt.getPhotoUrls() != null && !rt.getPhotoUrls().isBlank()
                        ? Arrays.asList(rt.getPhotoUrls().split(",")) : List.of(),
                inclusions
        );
    }

    /**
     * Enforce per-room capacity based on sharing type so a host can't configure
     * a PRIVATE room with maxGuests=5. Booking-service depends on maxGuests being
     * a truthful per-room cap when auto-scaling the room count from guest count.
     */
    private void validateMaxGuestsAgainstSharing(String sharingType, Integer maxGuests) {
        if (sharingType == null || maxGuests == null) return;
        int cap = switch (sharingType) {
            case "PRIVATE" -> 2;          // king-bed private room: up to 2 adults
            case "TWO_SHARING" -> 2;
            case "THREE_SHARING" -> 3;
            case "FOUR_SHARING" -> 4;
            case "DORMITORY" -> 12;       // bed-level cap enforced elsewhere
            default -> Integer.MAX_VALUE;
        };
        if (maxGuests < 1) {
            throw new IllegalArgumentException("maxGuests must be at least 1");
        }
        if (maxGuests > cap) {
            throw new IllegalArgumentException(
                    "maxGuests " + maxGuests + " exceeds per-room capacity " + cap
                    + " for sharingType " + sharingType + ". Use a larger sharing type or split into multiple room types.");
        }
    }

    private Integer computeTotalBeds(String sharingType, int roomCount) {
        int bedsPerRoom = switch (sharingType) {
            case "PRIVATE" -> 1;
            case "TWO_SHARING" -> 2;
            case "THREE_SHARING" -> 3;
            case "FOUR_SHARING" -> 4;
            case "DORMITORY" -> 6; // default dorm size
            default -> 1;
        };
        return bedsPerRoom * roomCount;
    }
}
