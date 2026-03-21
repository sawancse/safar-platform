package com.safar.search.kafka;

import com.safar.search.document.ListingDocument;
import com.safar.search.service.ListingServiceClient;
import com.safar.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingIndexConsumer {

    private final SearchService searchService;
    private final ListingServiceClient listingClient;

    /**
     * Auto-index on verify → listing appears in search immediately.
     */
    @KafkaListener(topics = "listing.verified", groupId = "search-service")
    public void onListingVerified(String message) {
        try {
            String listingId = extractId(message);
            ListingDocument doc = listingClient.getListingDocument(UUID.fromString(listingId));
            if (doc != null) {
                doc.setIsVerified(true);
                doc.setIndexedAt(LocalDateTime.now());
                searchService.indexListing(doc);
                log.info("Auto-indexed listing {} on verify", listingId);
            }
        } catch (Exception e) {
            log.error("Failed to index on listing.verified: {}", e.getMessage());
        }
    }

    /**
     * Remove from ES on archive/suspend → disappears from search immediately.
     */
    @KafkaListener(topics = {"listing.archived", "listing.suspended"}, groupId = "search-service")
    public void onListingRemoved(String message) {
        try {
            String listingId = extractId(message);
            searchService.deleteListing(listingId);
            log.info("Removed listing {} from ES (archived/suspended)", listingId);
        } catch (Exception e) {
            log.error("Failed to remove listing from ES: {}", e.getMessage());
        }
    }

    /**
     * When a review is created, recalculate avgRating and reviewCount in ES.
     */
    @KafkaListener(topics = "review.created", groupId = "search-review-group")
    public void onReviewCreated(String message) {
        try {
            String listingId = extractJsonString(message, "listingId");
            int newRating    = extractJsonInt(message, "rating");
            if (listingId == null) return;

            ListingDocument doc = searchService.getById(listingId);
            if (doc == null) {
                log.warn("Listing {} not found in ES for review rating update", listingId);
                return;
            }

            int prevCount  = doc.getReviewCount() != null ? doc.getReviewCount() : 0;
            double prevAvg = doc.getAvgRating()   != null ? doc.getAvgRating()   : 0.0;

            int    newCount = prevCount + 1;
            double newAvg   = ((prevAvg * prevCount) + newRating) / newCount;
            newAvg = Math.round(newAvg * 10.0) / 10.0; // 1 decimal place

            doc.setReviewCount(newCount);
            doc.setAvgRating(newAvg);
            searchService.indexListing(doc);
            log.info("Updated rating for listing {}: avg={} count={}", listingId, newAvg, newCount);
        } catch (Exception e) {
            log.error("Failed to update rating from review.created: {}", e.getMessage());
        }
    }

    private String extractId(String message) {
        String id = extractJsonString(message, "listingId");
        if (id != null) return id;
        if (message.contains("|")) return message.split("\\|")[0].trim();
        return message.trim().replaceAll("\"", "");
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    private int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)))) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }
}
