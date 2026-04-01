package com.safar.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyDonationRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {}
