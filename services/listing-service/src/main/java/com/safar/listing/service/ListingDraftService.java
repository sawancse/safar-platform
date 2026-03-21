package com.safar.listing.service;

import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.dto.ListingResponse;
import com.safar.listing.entity.ListingDraft;
import com.safar.listing.entity.enums.DraftStatus;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.repository.ListingDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingDraftService {

    private final ListingDraftRepository draftRepository;
    private final ListingService listingService;

    private static final Map<String, String> TITLE_TEMPLATES = Map.of(
            "HOME", "Cozy Home in %s",
            "ROOM", "Private Room in %s",
            "COMMERCIAL", "Commercial Space in %s"
    );

    private static final Map<String, String> DESCRIPTION_TEMPLATES = Map.of(
            "HOME", "A beautiful and fully-furnished home located in the heart of %s. Perfect for families and groups looking for a comfortable stay.",
            "ROOM", "A well-maintained private room in %s. Ideal for solo travellers and couples seeking an affordable yet comfortable stay.",
            "COMMERCIAL", "A versatile commercial space in %s. Suitable for meetings, events, and co-working setups."
    );

    private static final Map<String, String> AMENITY_DEFAULTS = Map.of(
            "HOME", "wifi,ac,kitchen,parking,tv,washing_machine",
            "ROOM", "wifi,ac,tv,attached_bathroom",
            "COMMERCIAL", "wifi,ac,projector,whiteboard,parking"
    );

    private static final Map<String, Long> CITY_BASE_PRICES = Map.of(
            "Mumbai", 350000L,
            "Delhi", 300000L,
            "Bangalore", 280000L,
            "Goa", 400000L,
            "Jaipur", 250000L
    );

    private static final long DEFAULT_PRICE_PAISE = 200000L;

    @Transactional
    public ListingDraft generateDraft(UUID hostId, String address, String type) {
        String normalizedType = type != null ? type.toUpperCase() : "HOME";

        String city = extractCity(address);
        String title = String.format(
                TITLE_TEMPLATES.getOrDefault(normalizedType, TITLE_TEMPLATES.get("HOME")), city);
        String description = String.format(
                DESCRIPTION_TEMPLATES.getOrDefault(normalizedType, DESCRIPTION_TEMPLATES.get("HOME")), city);
        String amenities = AMENITY_DEFAULTS.getOrDefault(normalizedType, AMENITY_DEFAULTS.get("HOME"));
        Long price = CITY_BASE_PRICES.getOrDefault(city, DEFAULT_PRICE_PAISE);

        ListingDraft draft = ListingDraft.builder()
                .hostId(hostId)
                .address(address)
                .type(normalizedType)
                .aiTitle(title)
                .aiDescription(description)
                .aiAmenities(amenities)
                .aiSuggestedPricePaise(price)
                .status(DraftStatus.DRAFT)
                .build();

        return draftRepository.save(draft);
    }

    @Transactional
    public ListingResponse convertToListing(UUID hostId, UUID draftId) {
        ListingDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new NoSuchElementException("Draft not found: " + draftId));

        if (!draft.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Draft does not belong to this host");
        }

        ListingType listingType;
        try {
            listingType = ListingType.valueOf(draft.getType());
        } catch (Exception e) {
            listingType = ListingType.HOME;
        }

        String city = extractCity(draft.getAddress());

        List<String> amenityList = draft.getAiAmenities() != null
                ? List.of(draft.getAiAmenities().split(","))
                : List.of();

        CreateListingRequest req = new CreateListingRequest(
                draft.getAiTitle(),
                draft.getAiDescription(),
                listingType,
                null,
                draft.getAddress() != null ? draft.getAddress() : "Address TBD",
                null,
                city,
                "TBD",
                "000000",
                null,
                null,
                2,
                null,
                null,
                null,
                amenityList,
                draft.getAiSuggestedPricePaise(),
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                // PG/Co-living fields
                null, null, null, null, null,
                // Hotel fields
                null, null, null, null
        );

        ListingResponse listing = listingService.createListing(hostId, req);

        draft.setStatus(DraftStatus.CONVERTED);
        draftRepository.save(draft);

        return listing;
    }

    public List<ListingDraft> getDrafts(UUID hostId) {
        return draftRepository.findByHostId(hostId);
    }

    private String extractCity(String address) {
        if (address == null || address.isBlank()) {
            return "City";
        }
        String[] parts = address.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 2].trim();
        }
        return parts[parts.length - 1].trim();
    }
}
