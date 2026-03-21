package com.safar.user.dto;

import com.safar.user.entity.enums.GuestSubStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GuestSubscriptionDto(
        UUID id,
        UUID guestId,
        GuestSubStatus status,
        OffsetDateTime trialEndsAt,
        OffsetDateTime nextBillingAt,
        OffsetDateTime createdAt
) {}
