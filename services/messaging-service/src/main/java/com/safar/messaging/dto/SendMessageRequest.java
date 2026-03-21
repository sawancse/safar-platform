package com.safar.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull(message = "listingId is required")
        UUID listingId,

        @NotNull(message = "recipientId is required")
        UUID recipientId,

        UUID bookingId,

        @NotBlank(message = "content is required")
        String content
) {}
