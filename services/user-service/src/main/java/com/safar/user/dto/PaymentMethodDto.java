package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentMethodDto(
        UUID id,
        String type,
        String label,
        Boolean isDefault,

        // UPI
        String upiId,

        // Card
        String cardLast4,
        String cardNetwork,
        String cardHolder,
        String cardExpiry,

        // Net Banking
        String bankName,
        String bankAccountLast4,

        OffsetDateTime createdAt
) {}
