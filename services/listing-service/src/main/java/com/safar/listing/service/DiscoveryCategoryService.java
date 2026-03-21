package com.safar.listing.service;

import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.DiscoveryCategory;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryCategoryService {

    private final ListingRepository listingRepository;

    /**
     * Get all available discovery categories with listing counts.
     */
    public List<Map<String, Object>> getCategoriesWithCounts() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DiscoveryCategory cat : DiscoveryCategory.values()) {
            long count = listingRepository.findAll().stream()
                    .filter(l -> l.getStatus() == ListingStatus.VERIFIED)
                    .filter(l -> l.getDiscoveryCategories() != null && l.getDiscoveryCategories().contains(cat.name()))
                    .count();
            result.add(Map.of(
                    "category", cat.name(),
                    "label", formatLabel(cat.name()),
                    "count", count
            ));
        }
        return result;
    }

    /**
     * Browse listings by discovery category.
     */
    public List<Listing> getListingsByCategory(DiscoveryCategory category, Pageable pageable) {
        return listingRepository.findAll(pageable).stream()
                .filter(l -> l.getStatus() == ListingStatus.VERIFIED)
                .filter(l -> l.getDiscoveryCategories() != null
                        && l.getDiscoveryCategories().contains(category.name()))
                .collect(Collectors.toList());
    }

    /**
     * Set discovery categories for a listing (max 3).
     */
    @Transactional
    public void setCategories(UUID listingId, UUID hostId, List<DiscoveryCategory> categories) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalStateException("Not the owner");
        }
        if (categories.size() > 3) {
            throw new IllegalArgumentException("Maximum 3 discovery categories allowed");
        }

        String cats = categories.stream().map(Enum::name).collect(Collectors.joining(","));
        listing.setDiscoveryCategories(cats);
        listingRepository.save(listing);
        log.info("Set discovery categories for listing {}: {}", listingId, cats);
    }

    /**
     * Auto-suggest categories based on listing properties.
     */
    public List<DiscoveryCategory> suggestCategories(Listing listing) {
        List<DiscoveryCategory> suggestions = new ArrayList<>();
        String city = listing.getCity() != null ? listing.getCity().toLowerCase() : "";
        String title = listing.getTitle() != null ? listing.getTitle().toLowerCase() : "";
        String desc = listing.getDescription() != null ? listing.getDescription().toLowerCase() : "";
        String combined = city + " " + title + " " + desc;

        // Hill stations
        if (city.matches(".*(manali|shimla|mussoorie|ooty|munnar|darjeeling|coorg|nainital|lonavala|mahabaleshwar).*")) {
            suggestions.add(DiscoveryCategory.HILL_STATIONS);
            suggestions.add(DiscoveryCategory.WORK_FROM_HILLS);
        }
        // Beach
        if (city.matches(".*(goa|pondicherry|gokarna|varkala|kovalam|alibaug|diu).*")
                || combined.contains("beach")) {
            suggestions.add(DiscoveryCategory.BEACH_HOUSES);
        }
        // Heritage
        if (city.matches(".*(jaipur|udaipur|jodhpur|jaisalmer|mysore|hampi).*")
                || combined.contains("haveli") || combined.contains("heritage") || combined.contains("palace")) {
            suggestions.add(DiscoveryCategory.HERITAGE_HAVELIS);
        }
        // Farm stay
        if (combined.contains("farm") || combined.contains("organic") || combined.contains("plantation")) {
            suggestions.add(DiscoveryCategory.FARM_STAYS);
        }
        // Pet friendly
        if (Boolean.TRUE.equals(listing.getPetFriendly())) {
            suggestions.add(DiscoveryCategory.PET_FRIENDLY);
        }
        // Pool
        if (combined.contains("pool") || combined.contains("swimming")) {
            suggestions.add(DiscoveryCategory.POOL_PARTIES);
        }
        // Budget
        if (listing.getBasePricePaise() != null && listing.getBasePricePaise() < 200000) {
            suggestions.add(DiscoveryCategory.BUDGET_GEMS);
        }
        // Weekend getaway (near metros)
        if (city.matches(".*(lonavala|alibaug|mahabaleshwar|coorg|nandi hills|rishikesh|matheran).*")) {
            suggestions.add(DiscoveryCategory.WEEKEND_GETAWAYS);
        }

        return suggestions.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private String formatLabel(String enumName) {
        return Arrays.stream(enumName.split("_"))
                .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
