package com.safar.listing.service;

import com.safar.listing.entity.AashrayCase;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AashrayMatchingService {

    private final ListingRepository listingRepository;

    public record MatchResult(UUID listingId, String title, String city, int matchScore, List<String> matchReasons) {}

    public List<MatchResult> findMatches(AashrayCase aashrayCase) {
        // Find Aashray-ready listings in preferred city
        List<Listing> candidates = listingRepository.findAll().stream()
                .filter(l -> Boolean.TRUE.equals(l.getAashrayReady()))
                .filter(l -> l.getStatus() == ListingStatus.VERIFIED)
                .filter(l -> aashrayCase.getPreferredCity().equalsIgnoreCase(l.getCity()))
                .collect(Collectors.toList());

        List<MatchResult> results = new ArrayList<>();

        for (Listing listing : candidates) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // 1. City match (required — already filtered) +20
            score += 20;
            reasons.add("City match: " + listing.getCity());

            // 2. Locality match +15
            if (aashrayCase.getPreferredLocality() != null && listing.getCity() != null) {
                String addr = (listing.getAddressLine1() + " " + listing.getAddressLine2()).toLowerCase();
                if (addr.contains(aashrayCase.getPreferredLocality().toLowerCase())) {
                    score += 15;
                    reasons.add("Locality match: " + aashrayCase.getPreferredLocality());
                }
            }

            // 3. Budget compatibility +20
            if (aashrayCase.getBudgetMaxPaise() == 0) {
                // NGO-funded — any Aashray listing works
                score += 20;
                reasons.add("NGO-funded (no budget constraint)");
            } else if (listing.getLongTermMonthlyPaise() != null && listing.getLongTermMonthlyPaise() > 0) {
                long effectivePrice = listing.getLongTermMonthlyPaise();
                if (listing.getAashrayDiscountPercent() != null && listing.getAashrayDiscountPercent() > 0) {
                    effectivePrice = effectivePrice * (100 - listing.getAashrayDiscountPercent()) / 100;
                }
                if (effectivePrice <= aashrayCase.getBudgetMaxPaise()) {
                    score += 20;
                    reasons.add("Within budget (₹" + effectivePrice / 100 + "/month)");
                } else {
                    score += 5;
                    reasons.add("Over budget but Aashray discount available");
                }
            }

            // 4. Family size fit +15
            if (listing.getMaxGuests() != null && listing.getMaxGuests() >= aashrayCase.getFamilySize()) {
                score += 15;
                reasons.add("Accommodates family of " + aashrayCase.getFamilySize());
            } else if (listing.getMaxGuests() != null) {
                score += 5;
                reasons.add("Partial fit (max " + listing.getMaxGuests() + " guests)");
            }

            // 5. Accessibility +10
            if (aashrayCase.getSpecialNeeds() != null) {
                String needs = aashrayCase.getSpecialNeeds().toLowerCase();
                if (needs.contains("wheelchair") || needs.contains("ground floor")) {
                    if (listing.getBedrooms() != null && listing.getBedrooms() <= 2) {
                        score += 10;
                        reasons.add("Ground floor / accessible");
                    }
                }
            }

            // 6. Language match +10
            if (aashrayCase.getLanguagesSpoken() != null) {
                // Simplified: Hindi is widely spoken
                if (aashrayCase.getLanguagesSpoken().toLowerCase().contains("hindi") ||
                    aashrayCase.getLanguagesSpoken().toLowerCase().contains("english")) {
                    score += 10;
                    reasons.add("Language compatibility");
                }
            }

            // 7. Aashray discount bonus +10
            if (listing.getAashrayDiscountPercent() != null && listing.getAashrayDiscountPercent() > 0) {
                score += 10;
                reasons.add("Aashray discount: " + listing.getAashrayDiscountPercent() + "%");
            }

            results.add(new MatchResult(listing.getId(), listing.getTitle(), listing.getCity(), Math.min(score, 100), reasons));
        }

        // Sort by score descending, return top 5
        results.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
        return results.stream().limit(5).collect(Collectors.toList());
    }
}
