package com.safar.listing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.SeekerProfile;
import com.safar.listing.entity.enums.*;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.SeekerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeekerProfileService {

    private final SeekerProfileRepository seekerRepository;
    private final ListingRepository listingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    public record SeekerMatchResult(UUID listingId, String title, String city, long pricePaise,
                                     int matchScore, List<String> matchReasons) {}

    @Transactional
    public SeekerProfile createProfile(SeekerProfile profile) {
        profile.setStatus(ProfileStatus.ACTIVE);
        profile.setMatchCount(0);
        SeekerProfile saved = seekerRepository.save(profile);
        kafkaTemplate.send("seeker.profile.created", saved.getId().toString(), toJson(saved));
        log.info("Seeker profile created: {} in {}", saved.getName(), saved.getPreferredCity());
        return saved;
    }

    @Transactional
    public SeekerProfile updateProfile(UUID id, SeekerProfile update) {
        SeekerProfile existing = seekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seeker profile not found: " + id));

        if (update.getPreferredCity() != null) existing.setPreferredCity(update.getPreferredCity());
        if (update.getPreferredLocality() != null) existing.setPreferredLocality(update.getPreferredLocality());
        if (update.getBudgetMinPaise() > 0) existing.setBudgetMinPaise(update.getBudgetMinPaise());
        if (update.getBudgetMaxPaise() > 0) existing.setBudgetMaxPaise(update.getBudgetMaxPaise());
        if (update.getPreferredSharing() != null) existing.setPreferredSharing(update.getPreferredSharing());
        if (update.getGenderPreference() != null) existing.setGenderPreference(update.getGenderPreference());
        if (update.getPreferredAmenities() != null) existing.setPreferredAmenities(update.getPreferredAmenities());
        if (update.getMoveInDate() != null) existing.setMoveInDate(update.getMoveInDate());
        if (update.getOccupation() != null) existing.setOccupation(update.getOccupation());
        if (update.getCompanyOrCollege() != null) existing.setCompanyOrCollege(update.getCompanyOrCollege());

        return seekerRepository.save(existing);
    }

    @Transactional
    public void deactivate(UUID id) {
        SeekerProfile profile = seekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seeker profile not found: " + id));
        profile.setStatus(ProfileStatus.INACTIVE);
        seekerRepository.save(profile);
    }

    public SeekerProfile getProfile(UUID id) {
        return seekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seeker profile not found: " + id));
    }

    public Page<SeekerProfile> getProfiles(ProfileStatus status, String city, Pageable pageable) {
        if (status != null && city != null) {
            return seekerRepository.findByStatusAndPreferredCity(status, city, pageable);
        } else if (status != null) {
            return seekerRepository.findByStatus(status, pageable);
        }
        return seekerRepository.findAll(pageable);
    }

    /**
     * Find listings matching a seeker's preferences.
     */
    public List<SeekerMatchResult> findMatchingListings(UUID seekerId) {
        SeekerProfile seeker = getProfile(seekerId);

        List<Listing> candidates = listingRepository.findAll().stream()
                .filter(l -> l.getStatus() == ListingStatus.VERIFIED)
                .filter(l -> seeker.getPreferredCity().equalsIgnoreCase(l.getCity()))
                .filter(l -> {
                    // Filter PG types for PG seekers
                    if (seeker.getSeekerType() == SeekerType.PG_SEEKER) {
                        return l.getType() == ListingType.PG;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<SeekerMatchResult> results = new ArrayList<>();

        for (Listing listing : candidates) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // City match +20
            score += 20;
            reasons.add("City: " + listing.getCity());

            // Budget match +25
            long price = listing.getBasePricePaise();
            if (listing.getLongTermMonthlyPaise() != null && listing.getLongTermMonthlyPaise() > 0) {
                price = listing.getLongTermMonthlyPaise();
            }
            if (seeker.getBudgetMaxPaise() > 0 && price <= seeker.getBudgetMaxPaise()) {
                score += 25;
                reasons.add("Within budget (₹" + price / 100 + ")");
            } else if (seeker.getBudgetMaxPaise() > 0 && price <= seeker.getBudgetMaxPaise() * 1.1) {
                score += 10;
                reasons.add("Slightly over budget");
            }

            // Gender policy match +15
            if (seeker.getGenderPreference() != null && listing.getGenderPolicy() != null) {
                if (seeker.getGenderPreference() == listing.getGenderPolicy() ||
                    listing.getGenderPolicy() == GenderPolicy.COED) {
                    score += 15;
                    reasons.add("Gender policy match");
                }
            } else {
                score += 10; // Neutral
            }

            // Locality match +15
            if (seeker.getPreferredLocality() != null && listing.getAddressLine1() != null) {
                if (listing.getAddressLine1().toLowerCase().contains(seeker.getPreferredLocality().toLowerCase())) {
                    score += 15;
                    reasons.add("Locality: " + seeker.getPreferredLocality());
                }
            }

            // Amenities match +15
            if (seeker.getPreferredAmenities() != null && listing.getAmenities() != null) {
                Set<String> wanted = Set.of(seeker.getPreferredAmenities().toLowerCase().split(","));
                long matched = listing.getAmenities().stream()
                        .filter(a -> wanted.contains(a.toLowerCase()))
                        .count();
                if (matched > 0) {
                    int amenityScore = (int) Math.min(15, matched * 5);
                    score += amenityScore;
                    reasons.add(matched + " amenities match");
                }
            }

            // Pet-friendly match +10
            if (seeker.isPetOwner() && Boolean.TRUE.equals(listing.getPetFriendly())) {
                score += 10;
                reasons.add("Pet-friendly");
            }

            results.add(new SeekerMatchResult(
                    listing.getId(), listing.getTitle(), listing.getCity(),
                    price, Math.min(score, 100), reasons
            ));
        }

        results.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
        return results.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Find seekers matching a listing's profile (host view).
     */
    public List<SeekerProfile> findSeekersForListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + listingId));

        long price = listing.getBasePricePaise();
        if (listing.getLongTermMonthlyPaise() != null && listing.getLongTermMonthlyPaise() > 0) {
            price = listing.getLongTermMonthlyPaise();
        }

        return seekerRepository.findMatchingSeekers(
                listing.getCity(), null, listing.getGenderPolicy(), price, price
        );
    }
}
