package com.safar.booking.service;

import com.safar.booking.dto.SubmitVideoReviewRequest;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.VideoReview;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.entity.enums.ModerationStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.VideoReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoReviewServiceTest {

    @Mock VideoReviewRepository reviewRepo;
    @Mock BookingRepository bookingRepo;
    @InjectMocks VideoReviewService service;

    private final UUID GUEST_ID   = UUID.randomUUID();
    private final UUID BOOKING_ID = UUID.randomUUID();
    private final UUID LISTING_ID = UUID.randomUUID();

    private Booking completedBooking() {
        return Booking.builder()
                .id(BOOKING_ID).guestId(GUEST_ID).listingId(LISTING_ID)
                .status(BookingStatus.COMPLETED)
                .bookingRef("TEST001")
                .checkIn(LocalDateTime.now().minusDays(3))
                .checkOut(LocalDateTime.now().minusDays(1))
                .hostId(UUID.randomUUID())
                .guestsCount(1).nights(2)
                .baseAmountPaise(100000L).insuranceAmountPaise(30000L)
                .gstAmountPaise(18000L).totalAmountPaise(148000L)
                .hostPayoutPaise(100000L)
                .build();
    }

    private SubmitVideoReviewRequest validReq(int durationSecs) {
        return new SubmitVideoReviewRequest(LISTING_ID, "s3/key/video.mp4",
                "https://cdn.example.com/video.mp4", durationSecs);
    }

    @Test
    void submitVideoReview_happyPath_autoApproved() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepo.existsByBookingIdAndGuestId(BOOKING_ID, GUEST_ID)).thenReturn(false);
        VideoReview saved = VideoReview.builder()
                .id(UUID.randomUUID()).bookingId(BOOKING_ID).guestId(GUEST_ID)
                .listingId(LISTING_ID).s3Key("s3/key/video.mp4")
                .durationSeconds(30).moderationStatus(ModerationStatus.APPROVED).build();
        when(reviewRepo.save(any())).thenReturn(saved);

        VideoReview result = service.submitVideoReview(BOOKING_ID, GUEST_ID, validReq(30));

        assertThat(result.getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
    }

    @Test
    void submitVideoReview_tooShort_throws400() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepo.existsByBookingIdAndGuestId(BOOKING_ID, GUEST_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.submitVideoReview(BOOKING_ID, GUEST_ID, validReq(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15");
    }

    @Test
    void submitVideoReview_tooLong_throws400() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepo.existsByBookingIdAndGuestId(BOOKING_ID, GUEST_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.submitVideoReview(BOOKING_ID, GUEST_ID, validReq(100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("90");
    }

    @Test
    void submitVideoReview_notCompleted_throwsIllegalState() {
        Booking pending = completedBooking();
        pending.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.submitVideoReview(BOOKING_ID, GUEST_ID, validReq(30)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitVideoReview_duplicate_throwsIllegalState() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepo.existsByBookingIdAndGuestId(BOOKING_ID, GUEST_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.submitVideoReview(BOOKING_ID, GUEST_ID, validReq(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    void getApprovedReviews_returnsFilteredList() {
        VideoReview v = VideoReview.builder().id(UUID.randomUUID())
                .listingId(LISTING_ID).moderationStatus(ModerationStatus.APPROVED).build();
        when(reviewRepo.findByListingIdAndModerationStatus(LISTING_ID, ModerationStatus.APPROVED))
                .thenReturn(List.of(v));

        var result = service.getApprovedReviews(LISTING_ID);
        assertThat(result).hasSize(1);
    }
}
