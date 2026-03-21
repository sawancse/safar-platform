package com.safar.listing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes review.created events and updates listing avg_rating and review_count.
     * Expected payload: {"reviewId":"...", "listingId":"...", "rating":5}
     */
    @KafkaListener(topics = "review.created", groupId = "listing-review-group")
    @Transactional
    public void onReviewCreated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            UUID listingId = UUID.fromString(json.get("listingId").asText());
            int newRating = json.get("rating").asInt();

            listingRepository.findById(listingId).ifPresentOrElse(listing -> {
                int currentCount = listing.getReviewCount() != null ? listing.getReviewCount() : 0;
                double currentAvg = listing.getAvgRating() != null ? listing.getAvgRating() : 0.0;

                // Recalculate average: ((old_avg * old_count) + new_rating) / (old_count + 1)
                int newCount = currentCount + 1;
                double newAvg = ((currentAvg * currentCount) + newRating) / newCount;
                newAvg = Math.round(newAvg * 10) / 10.0; // Round to 1 decimal

                listing.setAvgRating(newAvg);
                listing.setReviewCount(newCount);
                listingRepository.save(listing);

                log.info("Updated listing {} rating: {}/{} reviews", listingId, newAvg, newCount);
            }, () -> log.warn("Listing {} not found for review.created event", listingId));
        } catch (Exception e) {
            log.error("Failed to process review.created event: {} — {}", message, e.getMessage());
        }
    }
}
