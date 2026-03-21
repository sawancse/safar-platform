package com.safar.booking.repository;

import com.safar.booking.entity.VideoReview;
import com.safar.booking.entity.enums.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoReviewRepository extends JpaRepository<VideoReview, UUID> {
    List<VideoReview> findByListingIdAndModerationStatus(UUID listingId, ModerationStatus status);
    List<VideoReview> findByBookingId(UUID bookingId);
    boolean existsByBookingIdAndGuestId(UUID bookingId, UUID guestId);
}
