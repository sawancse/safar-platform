package com.safar.user.dto;

import java.util.UUID;

public record WalletDto(
        UUID guestId,
        long balancePaise,
        long lifetimeEarnedPaise
) {}
