package com.safar.payment.dto;

public record DonationOrderResponse(
        String donationRef,
        String razorpayOrderId,
        String razorpaySubscriptionId,
        Long amountPaise,
        String currency,
        String razorpayKeyId,
        String frequency
) {}
