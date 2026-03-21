package com.safar.review.service;

import com.safar.review.dto.CreateReviewRequest;
import com.safar.review.dto.HostReviewStatsResponse;
import com.safar.review.dto.ReviewResponse;
import com.safar.review.dto.ReviewStatsResponse;
import com.safar.review.entity.Review;
import com.safar.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepo;
    @Mock BookingServiceClient bookingClient;
    @Mock KafkaTemplate<String, String> kafka;

    @InjectMocks ReviewService reviewService;

    private final UUID guestId    = UUID.randomUUID();
    private final UUID hostId     = UUID.randomUUID();
    private final UUID listingId  = UUID.randomUUID();
    private final UUID bookingId  = UUID.randomUUID();

    private CreateReviewRequest simpleReq(UUID bookingId, short rating, String comment) {
        return new CreateReviewRequest(bookingId, rating, comment, null,
                null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private CreateReviewRequest fullReq(UUID bookingId, short rating, String comment) {
        return new CreateReviewRequest(bookingId, rating, comment, null,
                (short) 5, (short) 4, (short) 4, (short) 5, (short) 5, (short) 4,
                (short) 5, (short) 4, (short) 5, (short) 3, null);
    }

    @Test
    void createReview_confirmedBooking_succeeds() {
        when(bookingClient.isConfirmedBookingOwner(bookingId, guestId)).thenReturn(true);
        when(bookingClient.getHostIdForBooking(bookingId)).thenReturn(hostId);
        when(bookingClient.getListingIdForBooking(bookingId)).thenReturn(listingId);
        when(bookingClient.getGuestNameForBooking(bookingId)).thenReturn("John Doe");
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(false);
        when(reviewRepo.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateReviewRequest req = simpleReq(bookingId, (short) 5, "Wonderful stay!");
        ReviewResponse resp = reviewService.createReview(guestId, req);

        assertThat(resp.rating()).isEqualTo((short) 5);
        assertThat(resp.guestId()).isEqualTo(guestId);
        assertThat(resp.hostId()).isEqualTo(hostId);
        assertThat(resp.listingId()).isEqualTo(listingId);
        assertThat(resp.guestName()).isEqualTo("John Doe");
        verify(kafka).send(eq("review.created"), anyString(), anyString());
    }

    @Test
    void createReview_withCategoryRatings_succeeds() {
        when(bookingClient.isConfirmedBookingOwner(bookingId, guestId)).thenReturn(true);
        when(bookingClient.getHostIdForBooking(bookingId)).thenReturn(hostId);
        when(bookingClient.getListingIdForBooking(bookingId)).thenReturn(listingId);
        when(bookingClient.getGuestNameForBooking(bookingId)).thenReturn("Jane");
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(false);
        when(reviewRepo.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateReviewRequest req = fullReq(bookingId, (short) 5, "Perfect!");
        ReviewResponse resp = reviewService.createReview(guestId, req);

        assertThat(resp.ratingCleanliness()).isEqualTo((short) 5);
        assertThat(resp.ratingLocation()).isEqualTo((short) 4);
    }

    @Test
    void createReview_duplicateBooking_throwsConflict() {
        when(bookingClient.isConfirmedBookingOwner(bookingId, guestId)).thenReturn(true);
        when(reviewRepo.existsByBookingId(bookingId)).thenReturn(true);

        CreateReviewRequest req = simpleReq(bookingId, (short) 4, "Great!");

        assertThatThrownBy(() -> reviewService.createReview(guestId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createReview_notBookingOwner_throwsForbidden() {
        when(bookingClient.isConfirmedBookingOwner(bookingId, guestId)).thenReturn(false);

        CreateReviewRequest req = simpleReq(bookingId, (short) 3, "Okay");

        assertThatThrownBy(() -> reviewService.createReview(guestId, req))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not confirmed");
    }

    @Test
    void getReviewsForListing_returnsPaginated() {
        Review r1 = Review.builder().id(UUID.randomUUID()).listingId(listingId)
                .guestId(guestId).hostId(hostId).bookingId(bookingId).rating((short) 5).build();
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepo.findByListingIdAndGuestReviewVisibleTrue(listingId, pageable))
                .thenReturn(new PageImpl<>(List.of(r1), pageable, 1));

        Page<ReviewResponse> result = reviewService.getReviewsForListing(listingId, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).rating()).isEqualTo((short) 5);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getReviewStats_returnsAggregationWithCategories() {
        when(reviewRepo.countByListingIdAndGuestReviewVisibleTrue(listingId)).thenReturn(3L);
        when(reviewRepo.avgRatingByListingId(listingId)).thenReturn(4.333333);
        when(reviewRepo.avgCleanlinessRatingByListingId(listingId)).thenReturn(4.5);
        when(reviewRepo.avgLocationRatingByListingId(listingId)).thenReturn(4.0);
        when(reviewRepo.avgValueRatingByListingId(listingId)).thenReturn(3.8);
        when(reviewRepo.avgCommunicationRatingByListingId(listingId)).thenReturn(4.7);
        when(reviewRepo.avgCheckInRatingByListingId(listingId)).thenReturn(5.0);
        when(reviewRepo.avgAccuracyRatingByListingId(listingId)).thenReturn(4.2);

        ReviewStatsResponse stats = reviewService.getReviewStats(listingId);

        assertThat(stats.listingId()).isEqualTo(listingId);
        assertThat(stats.totalReviews()).isEqualTo(3);
        assertThat(stats.averageRating()).isEqualTo(4.3);
        assertThat(stats.avgCleanliness()).isEqualTo(4.5);
        assertThat(stats.avgLocation()).isEqualTo(4.0);
    }

    @Test
    void getReviewStats_noReviews_returnsZero() {
        when(reviewRepo.countByListingIdAndGuestReviewVisibleTrue(listingId)).thenReturn(0L);

        ReviewStatsResponse stats = reviewService.getReviewStats(listingId);

        assertThat(stats.totalReviews()).isEqualTo(0);
        assertThat(stats.averageRating()).isEqualTo(0.0);
        assertThat(stats.avgCleanliness()).isNull();
    }

    @Test
    void addHostReply_ownerHost_succeeds() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 4).build();

        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse resp = reviewService.addHostReply(reviewId, hostId, "Thank you!");

        assertThat(resp.reply()).isEqualTo("Thank you!");
        assertThat(resp.repliedAt()).isNotNull();
        verify(kafka).send(eq("review.replied"), anyString(), anyString());
    }

    @Test
    void addHostReply_update_setsReplyUpdatedAt() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 4)
                .reply("Old reply").repliedAt(OffsetDateTime.now()).build();

        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse resp = reviewService.addHostReply(reviewId, hostId, "Updated reply");

        assertThat(resp.reply()).isEqualTo("Updated reply");
        assertThat(resp.replyUpdatedAt()).isNotNull();
    }

    @Test
    void addHostReply_wrongHost_throwsForbidden() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 3).build();

        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        UUID wrongHost = UUID.randomUUID();
        assertThatThrownBy(() -> reviewService.addHostReply(reviewId, wrongHost, "Hi"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only the listing host");
    }

    @Test
    void deleteHostReply_succeeds() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 4)
                .reply("A reply").repliedAt(OffsetDateTime.now()).build();

        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.deleteHostReply(reviewId, hostId);

        assertThat(review.getReply()).isNull();
        assertThat(review.getRepliedAt()).isNull();
    }

    @Test
    void deleteHostReply_noReply_throwsConflict() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 4).build();

        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteHostReply(reviewId, hostId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No reply exists");
    }

    @Test
    void getHostReviews_all_returnsPaginated() {
        Review r1 = Review.builder().id(UUID.randomUUID()).hostId(hostId)
                .guestId(guestId).listingId(listingId).bookingId(bookingId).rating((short) 5).build();
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepo.findByHostId(hostId, pageable))
                .thenReturn(new PageImpl<>(List.of(r1), pageable, 1));

        Page<ReviewResponse> result = reviewService.getHostReviews(hostId, "all", pageable);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getHostReviews_pending_filtersUnreplied() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepo.findByHostIdAndReplyIsNull(hostId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<ReviewResponse> result = reviewService.getHostReviews(hostId, "pending", pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getHostReviewStats_returnsCounts() {
        when(reviewRepo.countByHostId(hostId)).thenReturn(10L);
        when(reviewRepo.countByHostIdAndReplyIsNull(hostId)).thenReturn(3L);

        HostReviewStatsResponse stats = reviewService.getHostReviewStats(hostId);

        assertThat(stats.totalReviews()).isEqualTo(10);
        assertThat(stats.pendingReplies()).isEqualTo(3);
        assertThat(stats.repliedReviews()).isEqualTo(7);
    }

    @Test
    void updateReview_byAuthor_succeeds() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).guestId(guestId)
                .hostId(hostId).listingId(listingId).bookingId(bookingId).rating((short) 3).comment("OK").build();
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse resp = reviewService.updateReview(reviewId, guestId, (short) 5, "Amazing!");
        assertThat(resp.rating()).isEqualTo((short) 5);
        assertThat(resp.comment()).isEqualTo("Amazing!");
    }

    @Test
    void updateReview_wrongGuest_throwsForbidden() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).guestId(guestId)
                .hostId(hostId).listingId(listingId).bookingId(bookingId).rating((short) 3).build();
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        UUID wrongGuest = UUID.randomUUID();
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, wrongGuest, (short) 5, "Nope"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteReview_byAuthor_succeeds() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).guestId(guestId)
                .hostId(hostId).listingId(listingId).bookingId(bookingId).rating((short) 4).build();
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, guestId);
        verify(reviewRepo).delete(review);
    }

    @Test
    void deleteReview_wrongGuest_throwsForbidden() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().id(reviewId).guestId(guestId)
                .hostId(hostId).listingId(listingId).bookingId(bookingId).rating((short) 4).build();
        when(reviewRepo.findById(reviewId)).thenReturn(Optional.of(review));

        UUID wrongGuest = UUID.randomUUID();
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, wrongGuest))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getMyReviews_returnsGuestReviews() {
        Review r1 = Review.builder().id(UUID.randomUUID()).guestId(guestId)
                .hostId(hostId).listingId(listingId).bookingId(bookingId).rating((short) 5).build();
        Review r2 = Review.builder().id(UUID.randomUUID()).guestId(guestId)
                .hostId(hostId).listingId(UUID.randomUUID()).bookingId(UUID.randomUUID()).rating((short) 4).build();
        when(reviewRepo.findByGuestId(guestId)).thenReturn(List.of(r1, r2));

        List<ReviewResponse> result = reviewService.getMyReviews(guestId);

        assertThat(result).hasSize(2);
    }
}
