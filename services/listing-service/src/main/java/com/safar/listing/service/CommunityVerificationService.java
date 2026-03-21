package com.safar.listing.service;

import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityVerificationService {

    private final ListingRepository listingRepository;
    private static final int REQUIRED_VERIFICATIONS = 3;

    @Transactional
    public CommunityVerifyResult submitVerification(UUID listingId, UUID guestId, boolean photosMatch, boolean amenitiesMatch, boolean feltSafe) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found"));

        // Don't allow host to verify their own listing
        if (listing.getHostId().equals(guestId)) {
            throw new IllegalStateException("Cannot verify your own listing");
        }

        int positiveCount = (photosMatch ? 1 : 0) + (amenitiesMatch ? 1 : 0) + (feltSafe ? 1 : 0);
        boolean positive = positiveCount >= 2; // 2 out of 3 = positive

        int currentCount = listing.getCommunityVerifyCount() != null ? listing.getCommunityVerifyCount() : 0;

        if (positive) {
            currentCount++;
            listing.setCommunityVerifyCount(currentCount);

            if (currentCount >= REQUIRED_VERIFICATIONS && !Boolean.TRUE.equals(listing.getCommunityVerified())) {
                listing.setCommunityVerified(true);
                log.info("Listing {} community verified after {} positive verifications", listingId, currentCount);
            }
        }

        listingRepository.save(listing);

        int milesEarned = 200; // reward for verification
        return new CommunityVerifyResult(positive, currentCount, Boolean.TRUE.equals(listing.getCommunityVerified()), milesEarned);
    }

    public record CommunityVerifyResult(boolean positive, int totalVerifications, boolean communityVerified, int milesEarned) {}
}
