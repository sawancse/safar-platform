package com.safar.review.controller;

import com.safar.review.dto.*;
import com.safar.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest req,
                                       Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return reviewService.createReview(guestId, req);
    }

    @GetMapping("/listing/{listingId}")
    public Page<ReviewResponse> getReviewsForListing(@PathVariable UUID listingId,
                                                     @RequestParam(required = false) Short rating,
                                                     @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return reviewService.getReviewsForListing(listingId, rating, pageable);
    }

    @GetMapping("/listing/{listingId}/stats")
    public ReviewStatsResponse getReviewStats(@PathVariable UUID listingId) {
        return reviewService.getReviewStats(listingId);
    }

    @PutMapping("/{reviewId}/reply")
    public ReviewResponse addHostReply(@PathVariable UUID reviewId,
                                       @RequestParam String reply,
                                       Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return reviewService.addHostReply(reviewId, hostId, reply);
    }

    @DeleteMapping("/{reviewId}/reply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHostReply(@PathVariable UUID reviewId, Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        reviewService.deleteHostReply(reviewId, hostId);
    }

    @GetMapping("/host/me")
    public Page<ReviewResponse> getHostReviews(@RequestParam(defaultValue = "all") String filter,
                                                @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
                                                Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return reviewService.getHostReviews(hostId, filter, pageable);
    }

    @GetMapping("/host/me/stats")
    public HostReviewStatsResponse getHostReviewStats(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return reviewService.getHostReviewStats(hostId);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(@PathVariable UUID reviewId,
                                       @RequestParam(required = false) Short rating,
                                       @RequestParam(required = false) String comment,
                                       Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return reviewService.updateReview(reviewId, guestId, rating, comment);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable UUID reviewId, Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        reviewService.deleteReview(reviewId, guestId);
    }

    @GetMapping("/me")
    public List<ReviewResponse> getMyReviews(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return reviewService.getMyReviews(guestId);
    }

    // ── Experience Reviews ──

    @PostMapping("/experience")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createExperienceReview(
            @Valid @RequestBody com.safar.review.dto.CreateExperienceReviewRequest req,
            Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return reviewService.createExperienceReview(guestId, req);
    }

    @GetMapping("/experience/{experienceId}")
    public Page<ReviewResponse> getReviewsForExperience(
            @PathVariable UUID experienceId,
            @RequestParam(required = false) Short rating,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return reviewService.getReviewsForExperience(experienceId, rating, pageable);
    }

    @GetMapping("/experience/{experienceId}/stats")
    public ReviewStatsResponse getExperienceReviewStats(@PathVariable UUID experienceId) {
        return reviewService.getExperienceReviewStats(experienceId);
    }

    /**
     * Host submits their review of the guest (double-blind).
     * Both reviews revealed simultaneously when both submitted or after 14-day deadline.
     */
    @PostMapping("/host-review/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse submitHostReview(
            @PathVariable UUID bookingId,
            @RequestParam Short rating,
            @RequestParam(required = false) String comment,
            Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return reviewService.submitHostReview(bookingId, hostId, rating, comment);
    }
}
