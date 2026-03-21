package com.safar.booking.service;

import com.safar.booking.dto.SubmitVideoReviewRequest;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.VideoReview;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.entity.enums.ModerationStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.VideoReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoReviewService {

    private final VideoReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;

    @Transactional
    public VideoReview submitVideoReview(UUID bookingId, UUID guestId,
                                         SubmitVideoReviewRequest req) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getGuestId().equals(guestId)) {
            throw new IllegalArgumentException("Booking does not belong to this guest");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Video reviews can only be submitted for completed bookings");
        }
        if (reviewRepo.existsByBookingIdAndGuestId(bookingId, guestId)) {
            throw new IllegalStateException("Video review already submitted for this booking");
        }

        int duration = req.durationSeconds();
        if (duration < 15 || duration > 90) {
            throw new IllegalArgumentException("Video review must be 15–90 seconds");
        }

        VideoReview review = VideoReview.builder()
                .bookingId(bookingId)
                .guestId(guestId)
                .listingId(req.listingId())
                .s3Key(req.s3Key())
                .cdnUrl(req.cdnUrl())
                .durationSeconds(duration)
                .moderationStatus(ModerationStatus.APPROVED) // auto-approve for MVP
                .build();

        return reviewRepo.save(review);
    }

    public List<VideoReview> getApprovedReviews(UUID listingId) {
        return reviewRepo.findByListingIdAndModerationStatus(listingId, ModerationStatus.APPROVED);
    }
}
