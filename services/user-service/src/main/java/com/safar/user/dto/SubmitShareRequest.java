package com.safar.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitShareRequest(
        @NotNull UUID bookingId,
        @NotNull String platform,
        String shareProofUrl
) {}
