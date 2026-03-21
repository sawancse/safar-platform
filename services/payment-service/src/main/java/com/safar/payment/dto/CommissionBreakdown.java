package com.safar.payment.dto;

import java.math.BigDecimal;

public record CommissionBreakdown(
    BigDecimal commissionRate,
    long commissionPaise,
    long hostPayoutPaise,
    long treatmentPaise,
    long accommodationPaise
) {}
