package com.safar.booking.controller;

import com.safar.booking.entity.VideoReview;
import com.safar.booking.service.VideoReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class VideoReviewController {

    private final VideoReviewService videoReviewService;

    /** Public — no auth required */
    @GetMapping("/{listingId}/video-reviews")
    public ResponseEntity<List<VideoReview>> getVideoReviews(@PathVariable UUID listingId) {
        return ResponseEntity.ok(videoReviewService.getApprovedReviews(listingId));
    }
}
