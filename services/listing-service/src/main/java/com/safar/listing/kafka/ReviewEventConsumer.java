package com.safar.listing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.repository.ExperienceRepository;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final ListingRepository listingRepository;
    private final ExperienceRepository experienceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes review.created events and updates listing/experience avg_rating and review_count.
     * Payload: {"reviewId":"...", "listingId":"...", "rating":5}
     *      or: {"reviewId":"...", "experienceId":"...", "targetType":"EXPERIENCE", "rating":5}
     */
    @KafkaListener(topics = "review.created", groupId = "listing-review-group")
    @Transactional
    public void onReviewCreated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            int newRating = json.get("rating").asInt();

            String targetType = json.has("targetType") ? json.get("targetType").asText() : "LISTING";

            if ("EXPERIENCE".equals(targetType)) {
                UUID experienceId = UUID.fromString(json.get("experienceId").asText());
                experienceRepository.findById(experienceId).ifPresentOrElse(experience -> {
                    int currentCount = experience.getReviewCount() != null ? experience.getReviewCount() : 0;
                    BigDecimal currentAvg = experience.getRating() != null ? experience.getRating() : BigDecimal.ZERO;

                    int newCount = currentCount + 1;
                    BigDecimal newAvg = currentAvg.multiply(BigDecimal.valueOf(currentCount))
                            .add(BigDecimal.valueOf(newRating))
                            .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

                    experience.setRating(newAvg);
                    experience.setReviewCount(newCount);
                    experienceRepository.save(experience);

                    log.info("Updated experience {} rating: {}/{} reviews", experienceId, newAvg, newCount);
                }, () -> log.warn("Experience {} not found for review.created event", experienceId));
            } else {
                UUID listingId = UUID.fromString(json.get("listingId").asText());
                listingRepository.findById(listingId).ifPresentOrElse(listing -> {
                    int currentCount = listing.getReviewCount() != null ? listing.getReviewCount() : 0;
                    double currentAvg = listing.getAvgRating() != null ? listing.getAvgRating() : 0.0;

                    int newCount = currentCount + 1;
                    double newAvg = ((currentAvg * currentCount) + newRating) / newCount;
                    newAvg = Math.round(newAvg * 10) / 10.0;

                    listing.setAvgRating(newAvg);
                    listing.setReviewCount(newCount);
                    listingRepository.save(listing);

                    log.info("Updated listing {} rating: {}/{} reviews", listingId, newAvg, newCount);
                }, () -> log.warn("Listing {} not found for review.created event", listingId));
            }
        } catch (Exception e) {
            log.error("Failed to process review.created event: {} — {}", message, e.getMessage());
        }
    }
}
