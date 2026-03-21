package com.safar.review.dto;

import com.safar.review.entity.Review;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID listingId,
        UUID guestId,
        UUID hostId,
        Short rating,
        String comment,
        String reply,
        OffsetDateTime repliedAt,
        OffsetDateTime replyUpdatedAt,
        OffsetDateTime createdAt,
        List<String> guestPhotoUrls,
        String guestName,
        Short ratingCleanliness,
        Short ratingLocation,
        Short ratingValue,
        Short ratingCommunication,
        Short ratingCheckIn,
        Short ratingAccuracy,
        Short ratingStaff,
        Short ratingFacilities,
        Short ratingComfort,
        Short ratingFreeWifi,
        String categoryComments
) {
    public static ReviewResponse from(Review r) {
        List<String> photoUrls = r.getGuestPhotoUrls() != null && !r.getGuestPhotoUrls().isBlank()
                ? Arrays.asList(r.getGuestPhotoUrls().split(","))
                : Collections.emptyList();
        return new ReviewResponse(
                r.getId(), r.getBookingId(), r.getListingId(),
                r.getGuestId(), r.getHostId(), r.getRating(),
                r.getComment(), r.getReply(), r.getRepliedAt(), r.getReplyUpdatedAt(),
                r.getCreatedAt(), photoUrls, r.getGuestName(),
                r.getRatingCleanliness(), r.getRatingLocation(), r.getRatingValue(),
                r.getRatingCommunication(), r.getRatingCheckIn(), r.getRatingAccuracy(),
                r.getRatingStaff(), r.getRatingFacilities(), r.getRatingComfort(), r.getRatingFreeWifi(),
                r.getCategoryComments()
        );
    }
}
