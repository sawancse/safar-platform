package com.safar.listing.service;

import com.safar.listing.dto.RoomTypeInclusionRequest;
import com.safar.listing.dto.RoomTypeInclusionResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.RoomType;
import com.safar.listing.entity.RoomTypeInclusion;
import com.safar.listing.entity.enums.ChargeType;
import com.safar.listing.entity.enums.InclusionCategory;
import com.safar.listing.entity.enums.InclusionMode;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.RoomTypeInclusionRepository;
import com.safar.listing.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTypeInclusionService {

    private final RoomTypeInclusionRepository inclusionRepo;
    private final RoomTypeRepository roomTypeRepo;
    private final ListingRepository listingRepo;

    @Transactional
    public RoomTypeInclusionResponse create(UUID listingId, UUID roomTypeId, UUID hostId,
                                             RoomTypeInclusionRequest req) {
        validateOwnership(listingId, roomTypeId, hostId);

        int nextSort = inclusionRepo.findByRoomTypeIdOrderBySortOrder(roomTypeId).size();

        RoomTypeInclusion inclusion = RoomTypeInclusion.builder()
                .roomTypeId(roomTypeId)
                .category(InclusionCategory.valueOf(req.category()))
                .name(req.name())
                .description(req.description())
                .inclusionMode(req.inclusionMode() != null
                        ? InclusionMode.valueOf(req.inclusionMode()) : InclusionMode.INCLUDED)
                .chargePaise(req.chargePaise() != null ? req.chargePaise() : 0L)
                .chargeType(req.chargeType() != null
                        ? ChargeType.valueOf(req.chargeType()) : ChargeType.PER_STAY)
                .discountPercent(req.discountPercent() != null ? req.discountPercent() : 0)
                .terms(req.terms())
                .isHighlight(req.isHighlight() != null ? req.isHighlight() : false)
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : nextSort)
                .build();

        RoomTypeInclusion saved = inclusionRepo.save(inclusion);
        log.info("Created inclusion '{}' ({}) for room type {}", saved.getName(), saved.getCategory(), roomTypeId);
        return toResponse(saved);
    }

    @Transactional
    public RoomTypeInclusionResponse update(UUID listingId, UUID roomTypeId, UUID inclusionId,
                                             UUID hostId, RoomTypeInclusionRequest req) {
        validateOwnership(listingId, roomTypeId, hostId);

        RoomTypeInclusion inclusion = inclusionRepo.findById(inclusionId)
                .orElseThrow(() -> new NoSuchElementException("Inclusion not found: " + inclusionId));
        if (!inclusion.getRoomTypeId().equals(roomTypeId)) {
            throw new IllegalArgumentException("Inclusion does not belong to this room type");
        }

        inclusion.setCategory(InclusionCategory.valueOf(req.category()));
        inclusion.setName(req.name());
        inclusion.setDescription(req.description());
        if (req.inclusionMode() != null) inclusion.setInclusionMode(InclusionMode.valueOf(req.inclusionMode()));
        if (req.chargePaise() != null) inclusion.setChargePaise(req.chargePaise());
        if (req.chargeType() != null) inclusion.setChargeType(ChargeType.valueOf(req.chargeType()));
        if (req.discountPercent() != null) inclusion.setDiscountPercent(req.discountPercent());
        inclusion.setTerms(req.terms());
        if (req.isHighlight() != null) inclusion.setIsHighlight(req.isHighlight());
        if (req.sortOrder() != null) inclusion.setSortOrder(req.sortOrder());

        RoomTypeInclusion saved = inclusionRepo.save(inclusion);
        log.info("Updated inclusion '{}' for room type {}", saved.getName(), roomTypeId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID listingId, UUID roomTypeId, UUID inclusionId, UUID hostId) {
        validateOwnership(listingId, roomTypeId, hostId);

        RoomTypeInclusion inclusion = inclusionRepo.findById(inclusionId)
                .orElseThrow(() -> new NoSuchElementException("Inclusion not found: " + inclusionId));
        if (!inclusion.getRoomTypeId().equals(roomTypeId)) {
            throw new IllegalArgumentException("Inclusion does not belong to this room type");
        }

        inclusionRepo.delete(inclusion);
        log.info("Deleted inclusion {} from room type {}", inclusionId, roomTypeId);
    }

    public List<RoomTypeInclusionResponse> getInclusions(UUID roomTypeId) {
        return inclusionRepo.findByRoomTypeIdAndIsActiveTrueOrderBySortOrder(roomTypeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoomTypeInclusionResponse> getAllInclusions(UUID roomTypeId) {
        return inclusionRepo.findByRoomTypeIdOrderBySortOrder(roomTypeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RoomTypeInclusionResponse> bulkCreate(UUID listingId, UUID roomTypeId, UUID hostId,
                                                       List<RoomTypeInclusionRequest> requests) {
        validateOwnership(listingId, roomTypeId, hostId);

        // Delete existing and replace
        inclusionRepo.deleteByRoomTypeId(roomTypeId);

        List<RoomTypeInclusion> inclusions = new java.util.ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            RoomTypeInclusionRequest req = requests.get(i);
            inclusions.add(RoomTypeInclusion.builder()
                    .roomTypeId(roomTypeId)
                    .category(InclusionCategory.valueOf(req.category()))
                    .name(req.name())
                    .description(req.description())
                    .inclusionMode(req.inclusionMode() != null
                            ? InclusionMode.valueOf(req.inclusionMode()) : InclusionMode.INCLUDED)
                    .chargePaise(req.chargePaise() != null ? req.chargePaise() : 0L)
                    .chargeType(req.chargeType() != null
                            ? ChargeType.valueOf(req.chargeType()) : ChargeType.PER_STAY)
                    .discountPercent(req.discountPercent() != null ? req.discountPercent() : 0)
                    .terms(req.terms())
                    .isHighlight(req.isHighlight() != null ? req.isHighlight() : false)
                    .sortOrder(req.sortOrder() != null ? req.sortOrder() : i)
                    .build());
        }

        List<RoomTypeInclusion> saved = inclusionRepo.saveAll(inclusions);
        log.info("Bulk created {} inclusions for room type {}", saved.size(), roomTypeId);
        return saved.stream().map(this::toResponse).toList();
    }

    // ── Private helpers ─────────────────────────────────────────

    private void validateOwnership(UUID listingId, UUID roomTypeId, UUID hostId) {
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
    }

    private RoomTypeInclusionResponse toResponse(RoomTypeInclusion i) {
        return new RoomTypeInclusionResponse(
                i.getId(),
                i.getRoomTypeId(),
                i.getCategory().name(),
                i.getName(),
                i.getDescription(),
                i.getInclusionMode().name(),
                i.getChargePaise(),
                i.getChargeType().name(),
                i.getDiscountPercent(),
                i.getTerms(),
                i.getIsHighlight(),
                i.getSortOrder(),
                i.getIsActive()
        );
    }
}
