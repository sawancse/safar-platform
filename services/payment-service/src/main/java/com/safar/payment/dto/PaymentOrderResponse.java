package com.safar.payment.dto;

public record PaymentOrderResponse(
        String razorpayOrderId,
        Long amountPaise,
        String currency,
        String razorpayKeyId
) {}
