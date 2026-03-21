package com.safar.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record TaxProfileRequest(
        String gstin,
        @NotBlank String pan,
        String businessName,
        String stateCode
) {}
