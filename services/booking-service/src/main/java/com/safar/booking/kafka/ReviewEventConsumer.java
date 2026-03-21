package com.safar.booking.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final BookingRepository bookingRepo;
    private final ObjectMapper objectMapper;

    /**
     * Consumes review.created events and marks the booking as reviewed.
     * Expected payload: {"reviewId":"...", "bookingId":"...", "listingId":"...", "rating":5}
     */
    @KafkaListener(topics = "review.created", groupId = "booking-review-group")
    @Transactional
    public void onReviewCreated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            JsonNode bookingIdNode = json.get("bookingId");
            if (bookingIdNode == null || bookingIdNode.isNull()) {
                log.warn("review.created event missing bookingId: {}", message);
                return;
            }

            UUID bookingId = UUID.fromString(bookingIdNode.asText());
            int rating = json.has("rating") ? json.get("rating").asInt() : 0;

            bookingRepo.findById(bookingId).ifPresentOrElse(booking -> {
                booking.setHasReview(true);
                booking.setReviewRating(rating);
                booking.setReviewedAt(OffsetDateTime.now());
                bookingRepo.save(booking);
                log.info("Booking {} marked as reviewed (rating={})", bookingId, rating);
            }, () -> log.warn("Booking {} not found for review.created event", bookingId));
        } catch (Exception e) {
            log.error("Failed to process review.created event: {} — {}", message, e.getMessage());
        }
    }
}
