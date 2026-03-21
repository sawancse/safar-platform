package com.safar.payment.dto;

import java.util.UUID;

public record TdsReport(
        UUID hostId,
        String pan,
        String period,
        long totalRevenuePaise,
        long tdsDeductedPaise,
        int invoiceCount
) {}
