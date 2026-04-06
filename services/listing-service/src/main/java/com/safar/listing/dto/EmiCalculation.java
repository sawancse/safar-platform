package com.safar.listing.dto;

import java.math.BigDecimal;

public record EmiCalculation(
        Long loanAmountPaise,
        BigDecimal interestRate,
        int tenureMonths,
        Long emiPaise,
        Long totalInterestPaise,
        Long totalAmountPaise
) {}
