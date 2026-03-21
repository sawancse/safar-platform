package com.safar.review.repository;

import com.safar.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBookingId(UUID bookingId);

    java.util.Optional<Review> findByBookingId(UUID bookingId);

    Page<Review> findByListingId(UUID listingId, Pageable pageable);

    Page<Review> findByListingIdAndGuestReviewVisibleTrue(UUID listingId, Pageable pageable);

    Page<Review> findByListingIdAndRatingAndGuestReviewVisibleTrue(UUID listingId, Short rating, Pageable pageable);

    long countByListingIdAndGuestReviewVisibleTrue(UUID listingId);

    List<Review> findByGuestId(UUID guestId);

    Page<Review> findByListingIdAndRating(UUID listingId, Short rating, Pageable pageable);

    long countByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.listingId = :listingId AND r.guestReviewVisible = true")
    double avgRatingByListingId(UUID listingId);

    Page<Review> findByHostId(UUID hostId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.hostId = :hostId AND r.reply IS NULL")
    Page<Review> findByHostIdAndReplyIsNull(UUID hostId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.hostId = :hostId AND r.reply IS NOT NULL")
    Page<Review> findByHostIdAndReplyIsNotNull(UUID hostId, Pageable pageable);

    long countByHostId(UUID hostId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.hostId = :hostId AND r.reply IS NULL")
    long countByHostIdAndReplyIsNull(UUID hostId);

    @Query("SELECT COALESCE(AVG(r.ratingCleanliness), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingCleanliness IS NOT NULL AND r.guestReviewVisible = true")
    double avgCleanlinessRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingLocation), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingLocation IS NOT NULL AND r.guestReviewVisible = true")
    double avgLocationRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingValue), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingValue IS NOT NULL AND r.guestReviewVisible = true")
    double avgValueRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingCommunication), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingCommunication IS NOT NULL AND r.guestReviewVisible = true")
    double avgCommunicationRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingCheckIn), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingCheckIn IS NOT NULL AND r.guestReviewVisible = true")
    double avgCheckInRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingAccuracy), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingAccuracy IS NOT NULL AND r.guestReviewVisible = true")
    double avgAccuracyRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingStaff), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingStaff IS NOT NULL AND r.guestReviewVisible = true")
    double avgStaffRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingFacilities), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingFacilities IS NOT NULL AND r.guestReviewVisible = true")
    double avgFacilitiesRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingComfort), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingComfort IS NOT NULL AND r.guestReviewVisible = true")
    double avgComfortRatingByListingId(UUID listingId);

    @Query("SELECT COALESCE(AVG(r.ratingFreeWifi), 0) FROM Review r WHERE r.listingId = :listingId AND r.ratingFreeWifi IS NOT NULL AND r.guestReviewVisible = true")
    double avgFreeWifiRatingByListingId(UUID listingId);
}
