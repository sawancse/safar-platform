package com.safar.payment.dto;

import java.util.UUID;

public record PnlStatement(
        UUID hostId,
        int year,
        long grossRevenuePaise,
        long expensesPaise,
        long platformFeesPaise,
        long tdsDeductedPaise,
        long netProfitPaise
) {}
