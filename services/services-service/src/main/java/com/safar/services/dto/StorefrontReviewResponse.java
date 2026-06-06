package com.safar.services.dto;

import com.safar.services.entity.EventBooking;

/**
 * A single public review for a vendor's storefront — the customer's rating +
 * comment left on a delivered event booking. Reviewer name is masked to a
 * first name + last initial for privacy.
 */
public record StorefrontReviewResponse(
        int rating,
        String comment,
        String reviewerName,
        String eventType,
        String date
) {
    public static StorefrontReviewResponse from(EventBooking e) {
        return new StorefrontReviewResponse(
                e.getRatingGiven() == null ? 0 : e.getRatingGiven(),
                e.getReviewComment(),
                maskName(e.getCustomerName()),
                e.getEventType(),
                e.getEventDate() != null ? e.getEventDate().toString()
                        : (e.getCreatedAt() != null ? e.getCreatedAt().toLocalDate().toString() : null)
        );
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Verified customer";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0];
        String firstCap = Character.toUpperCase(first.charAt(0)) + (first.length() > 1 ? first.substring(1) : "");
        if (parts.length == 1) return firstCap;
        return firstCap + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
