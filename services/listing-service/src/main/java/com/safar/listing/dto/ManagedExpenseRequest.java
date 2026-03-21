package com.safar.listing.dto;

import java.util.UUID;

public record ManagedExpenseRequest(
        UUID bookingId,
        String expenseType,
        Long amountPaise,
        String description,
        String receiptUrl
) {}
