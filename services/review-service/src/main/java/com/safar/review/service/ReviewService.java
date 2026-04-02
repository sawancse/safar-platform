package com.safar.review.service;

import com.safar.review.dto.*;
import com.safar.review.entity.Review;
import com.safar.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final BookingServiceClient bookingClient;
    private final KafkaTemplate<String, String> kafka;

    @Transactional
    public ReviewResponse createReview(UUID guestId, CreateReviewRequest req) {
        if (!bookingClient.isConfirmedBookingOwner(req.bookingId(), guestId)) {
            throw new SecurityException("Booking not found or not confirmed for this guest");
        }

        if (reviewRepo.existsByBookingId(req.bookingId())) {
            throw new IllegalStateException("A review already exists for this booking");
        }

        UUID hostId    = bookingClient.getHostIdForBooking(req.bookingId());
        UUID listingId = bookingClient.getListingIdForBooking(req.bookingId());

        // Resolve guest name from booking
        String guestName = bookingClient.getGuestNameForBooking(req.bookingId());

        String photoUrls = req.guestPhotoUrls() != null && !req.guestPhotoUrls().isEmpty()
                ? String.join(",", req.guestPhotoUrls()) : null;

        // Double-blind: set review deadline 14 days after checkout
        OffsetDateTime checkOut = bookingClient.getCheckOutForBooking(req.bookingId());
        OffsetDateTime deadline = checkOut != null ? checkOut.plusDays(14) : OffsetDateTime.now().plusDays(14);

        Review review = Review.builder()
                .bookingId(req.bookingId())
                .listingId(listingId)
                .guestId(guestId)
                .hostId(hostId)
                .rating(req.rating())
                .comment(req.comment())
                .guestPhotoUrls(photoUrls)
                .guestName(guestName)
                .ratingCleanliness(req.ratingCleanliness())
                .ratingLocation(req.ratingLocation())
                .ratingValue(req.ratingValue())
                .ratingCommunication(req.ratingCommunication())
                .ratingCheckIn(req.ratingCheckIn())
                .ratingAccuracy(req.ratingAccuracy())
                .ratingStaff(req.ratingStaff())
                .ratingFacilities(req.ratingFacilities())
                .ratingComfort(req.ratingComfort())
                .ratingFreeWifi(req.ratingFreeWifi())
                .categoryComments(req.categoryComments())
                .reviewDeadline(deadline)
                .guestReviewVisible(true) // guest reviews visible immediately (Airbnb model)
                .hostReviewVisible(false) // host review hidden until both submit or deadline
                .build();

        review = reviewRepo.save(review);

        // Check if host already reviewed → reveal both
        tryRevealDoubleBlind(review);

        String event = String.format(
                "{\"reviewId\":\"%s\",\"bookingId\":\"%s\",\"listingId\":\"%s\",\"rating\":%d}",
                review.getId(), req.bookingId(), listingId, req.rating());
        kafka.send("review.created", review.getId().toString(), event);
        log.info("Review {} created for listing {}", review.getId(), listingId);

        return ReviewResponse.from(review);
    }

    public Page<ReviewResponse> getReviewsForListing(UUID listingId, Short ratingFilter, Pageable pageable) {
        Page<Review> reviews = ratingFilter != null
                ? reviewRepo.findByListingIdAndRatingAndGuestReviewVisibleTrue(listingId, ratingFilter, pageable)
                : reviewRepo.findByListingIdAndGuestReviewVisibleTrue(listingId, pageable);
        return reviews.map(ReviewResponse::from);
    }

    public ReviewStatsResponse getReviewStats(UUID listingId) {
        long count = reviewRepo.countByListingIdAndGuestReviewVisibleTrue(listingId);
        if (count == 0) {
            return new ReviewStatsResponse(listingId, 0, 0.0);
        }
        double avg = Math.round(reviewRepo.avgRatingByListingId(listingId) * 10) / 10.0;
        double avgCleanliness   = Math.round(reviewRepo.avgCleanlinessRatingByListingId(listingId) * 10) / 10.0;
        double avgLocation      = Math.round(reviewRepo.avgLocationRatingByListingId(listingId) * 10) / 10.0;
        double avgValue         = Math.round(reviewRepo.avgValueRatingByListingId(listingId) * 10) / 10.0;
        double avgCommunication = Math.round(reviewRepo.avgCommunicationRatingByListingId(listingId) * 10) / 10.0;
        double avgCheckIn       = Math.round(reviewRepo.avgCheckInRatingByListingId(listingId) * 10) / 10.0;
        double avgAccuracy      = Math.round(reviewRepo.avgAccuracyRatingByListingId(listingId) * 10) / 10.0;
        double avgStaff         = Math.round(reviewRepo.avgStaffRatingByListingId(listingId) * 10) / 10.0;
        double avgFacilities    = Math.round(reviewRepo.avgFacilitiesRatingByListingId(listingId) * 10) / 10.0;
        double avgComfort       = Math.round(reviewRepo.avgComfortRatingByListingId(listingId) * 10) / 10.0;
        double avgFreeWifi      = Math.round(reviewRepo.avgFreeWifiRatingByListingId(listingId) * 10) / 10.0;

        return new ReviewStatsResponse(listingId, count, avg,
                avgCleanliness > 0 ? avgCleanliness : null,
                avgLocation > 0 ? avgLocation : null,
                avgValue > 0 ? avgValue : null,
                avgCommunication > 0 ? avgCommunication : null,
                avgCheckIn > 0 ? avgCheckIn : null,
                avgAccuracy > 0 ? avgAccuracy : null,
                avgStaff > 0 ? avgStaff : null,
                avgFacilities > 0 ? avgFacilities : null,
                avgComfort > 0 ? avgComfort : null,
                avgFreeWifi > 0 ? avgFreeWifi : null);
    }

    @Transactional
    public ReviewResponse addHostReply(UUID reviewId, UUID hostId, String reply) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        if (!review.getHostId().equals(hostId)) {
            throw new SecurityException("Only the listing host can reply to this review");
        }

        boolean isUpdate = review.getReply() != null;
        review.setReply(reply);
        if (isUpdate) {
            review.setReplyUpdatedAt(OffsetDateTime.now());
        } else {
            review.setRepliedAt(OffsetDateTime.now());
        }
        review = reviewRepo.save(review);

        // Publish Kafka event for notification
        String event = String.format(
                "{\"reviewId\":\"%s\",\"listingId\":\"%s\",\"guestId\":\"%s\",\"hostId\":\"%s\"}",
                review.getId(), review.getListingId(), review.getGuestId(), hostId);
        kafka.send("review.replied", review.getId().toString(), event);
        log.info("Host {} replied to review {}", hostId, reviewId);

        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteHostReply(UUID reviewId, UUID hostId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        if (!review.getHostId().equals(hostId)) {
            throw new SecurityException("Only the listing host can delete this reply");
        }

        if (review.getReply() == null) {
            throw new IllegalStateException("No reply exists on this review");
        }

        review.setReply(null);
        review.setRepliedAt(null);
        review.setReplyUpdatedAt(null);
        reviewRepo.save(review);
        log.info("Host {} deleted reply on review {}", hostId, reviewId);
    }

    public Page<ReviewResponse> getHostReviews(UUID hostId, String filter, Pageable pageable) {
        return switch (filter) {
            case "pending" -> reviewRepo.findByHostIdAndReplyIsNull(hostId, pageable).map(ReviewResponse::from);
            case "replied" -> reviewRepo.findByHostIdAndReplyIsNotNull(hostId, pageable).map(ReviewResponse::from);
            default -> reviewRepo.findByHostId(hostId, pageable).map(ReviewResponse::from);
        };
    }

    public HostReviewStatsResponse getHostReviewStats(UUID hostId) {
        long total = reviewRepo.countByHostId(hostId);
        long pending = reviewRepo.countByHostIdAndReplyIsNull(hostId);
        return new HostReviewStatsResponse(total, pending, total - pending);
    }

    public List<ReviewResponse> getMyReviews(UUID guestId) {
        return reviewRepo.findByGuestId(guestId).stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID guestId, Short rating, String comment) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getGuestId().equals(guestId)) {
            throw new SecurityException("Only the review author can edit this review");
        }
        if (rating != null) review.setRating(rating);
        if (comment != null) review.setComment(comment);
        return ReviewResponse.from(reviewRepo.save(review));
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID guestId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getGuestId().equals(guestId)) {
            throw new SecurityException("Only the review author can delete this review");
        }
        reviewRepo.delete(review);
        log.info("Review {} deleted by guest {}", reviewId, guestId);
    }

    // ────────────────────────────────────────────────────────────
    // Experience Reviews
    // ────────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse createExperienceReview(UUID guestId, com.safar.review.dto.CreateExperienceReviewRequest req) {
        if (reviewRepo.existsByBookingId(req.experienceBookingId())) {
            throw new IllegalStateException("A review already exists for this experience booking");
        }

        String photoUrls = req.guestPhotoUrls() != null && !req.guestPhotoUrls().isEmpty()
                ? String.join(",", req.guestPhotoUrls()) : null;

        Review review = Review.builder()
                .bookingId(req.experienceBookingId())
                .targetType("EXPERIENCE")
                .experienceId(req.experienceId())
                .hostId(req.hostId())
                .guestId(guestId)
                .rating(req.rating())
                .comment(req.comment())
                .guestPhotoUrls(photoUrls)
                .ratingValue(req.ratingValue())
                .ratingCommunication(req.ratingCommunication())
                .ratingAccuracy(req.ratingAccuracy())
                .reviewDeadline(OffsetDateTime.now().plusDays(14))
                .guestReviewVisible(true)
                .hostReviewVisible(false)
                .build();

        review = reviewRepo.save(review);

        String event = String.format(
                "{\"reviewId\":\"%s\",\"experienceId\":\"%s\",\"targetType\":\"EXPERIENCE\",\"rating\":%d}",
                review.getId(), req.experienceId(), req.rating());
        kafka.send("review.created", review.getId().toString(), event);
        log.info("Experience review {} created for experience {}", review.getId(), req.experienceId());

        return ReviewResponse.from(review);
    }

    public Page<ReviewResponse> getReviewsForExperience(UUID experienceId, Short ratingFilter, Pageable pageable) {
        Page<Review> reviews = ratingFilter != null
                ? reviewRepo.findByExperienceIdAndRatingAndGuestReviewVisibleTrue(experienceId, ratingFilter, pageable)
                : reviewRepo.findByExperienceIdAndGuestReviewVisibleTrue(experienceId, pageable);
        return reviews.map(ReviewResponse::from);
    }

    public ReviewStatsResponse getExperienceReviewStats(UUID experienceId) {
        long count = reviewRepo.countByExperienceIdAndGuestReviewVisibleTrue(experienceId);
        if (count == 0) {
            return new ReviewStatsResponse(experienceId, 0, 0.0);
        }
        double avg = Math.round(reviewRepo.avgRatingByExperienceId(experienceId) * 10) / 10.0;
        return new ReviewStatsResponse(experienceId, count, avg);
    }

    // ────────────────────────────────────────────────────────────
    // Feature 5: Double-Blind Reviews (Airbnb model)
    // ────────────────────────────────────────────────────────────

    /**
     * Host submits their review of the guest (independent of guest review).
     */
    @Transactional
    public ReviewResponse submitHostReview(UUID bookingId, UUID hostId, Short rating, String comment) {
        Review review = reviewRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No review record for booking: " + bookingId));

        if (!review.getHostId().equals(hostId)) {
            throw new SecurityException("Only the listing host can review this guest");
        }
        if (review.getHostRating() != null) {
            throw new IllegalStateException("Host review already submitted for this booking");
        }

        review.setHostRating(rating);
        review.setHostComment(comment);
        review.setHostReviewedAt(OffsetDateTime.now());

        review = reviewRepo.save(review);

        // Check if guest already reviewed → reveal both
        tryRevealDoubleBlind(review);

        log.info("Host {} submitted review for booking {}", hostId, bookingId);
        return ReviewResponse.from(review);
    }

    /**
     * Reveal both reviews when BOTH guest and host have submitted,
     * or when the 14-day deadline expires.
     */
    private void tryRevealDoubleBlind(Review review) {
        boolean guestDone = review.getRating() != null;
        boolean hostDone = review.getHostRating() != null;

        if (guestDone && hostDone) {
            review.setGuestReviewVisible(true);
            review.setHostReviewVisible(true);
            review.setBothRevealedAt(OffsetDateTime.now());
            reviewRepo.save(review);
            log.info("Double-blind reviews revealed for booking {}", review.getBookingId());
        }
    }

    /**
     * Scheduled: reveal reviews past their 14-day deadline.
     * Runs daily at 1 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void revealExpiredReviews() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Review> expired = reviewRepo.findAll().stream()
                .filter(r -> r.getReviewDeadline() != null && r.getReviewDeadline().isBefore(now))
                .filter(r -> !Boolean.TRUE.equals(r.getGuestReviewVisible()))
                .toList();

        for (Review review : expired) {
            if (review.getRating() != null) review.setGuestReviewVisible(true);
            if (review.getHostRating() != null) review.setHostReviewVisible(true);
            if (review.getRating() != null || review.getHostRating() != null) {
                review.setBothRevealedAt(now);
            }
            reviewRepo.save(review);
        }

        if (!expired.isEmpty()) {
            log.info("Revealed {} expired double-blind reviews", expired.size());
        }
    }
}
