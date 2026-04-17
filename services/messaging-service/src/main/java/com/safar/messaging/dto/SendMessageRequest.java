package com.safar.messaging.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull(message = "listingId is required")
        UUID listingId,

        @NotNull(message = "recipientId is required")
        UUID recipientId,

        UUID bookingId,

        String content,

        // Message type: TEXT (default), FILE, IMAGE, LOCATION
        String messageType,

        // Attachment fields (for FILE/IMAGE)
        String attachmentUrl,
        String attachmentName,
        Long attachmentSize,
        String attachmentType,

        // Location fields (for LOCATION)
        Double latitude,
        Double longitude,
        String locationLabel
) {}
