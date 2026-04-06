package com.safar.listing.dto;

import java.util.UUID;

public record LoanApplicationRequest(
        UUID bankId,
        Long loanAmountPaise,
        Integer tenureMonths,
        UUID propertyId,
        String applicantName,
        String applicantPhone,
        String applicantEmail
) {}
