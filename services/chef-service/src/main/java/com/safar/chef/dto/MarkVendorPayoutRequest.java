package com.safar.chef.dto;

public record MarkVendorPayoutRequest(
        String payoutRef,   // NEFT UTR / Razorpay payout id
        Long payoutPaise    // optional override; defaults to assignment.payoutPaise
) {}
