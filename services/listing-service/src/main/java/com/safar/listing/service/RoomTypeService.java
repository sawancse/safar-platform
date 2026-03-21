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
                .map(rt -> toResponse(rt, null))
                .toList();
    }

    public List<RoomTypeResponse> getAvailableRoomTypes(UUID listingId, LocalDate checkIn, LocalDate checkOut) {
        List<RoomType> roomTypes = roomTypeRepo.findByListingIdOrderBySortOrder(listingId);
        List<RoomTypeResponse> result = new ArrayList<>();

        for (RoomType rt : roomTypes) {
            int minAvailable = computeMinAvailable(rt.getId(), checkIn, checkOut, rt.getCount());
            result.add(toResponse(rt, minAvailable));
        }

        return result;
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
                rt.getPrimaryPhotoUrl(),
                rt.getPhotoUrls() != null && !rt.getPhotoUrls().isBlank()
                        ? Arrays.asList(rt.getPhotoUrls().split(",")) : List.of(),
                inclusions
        );
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
