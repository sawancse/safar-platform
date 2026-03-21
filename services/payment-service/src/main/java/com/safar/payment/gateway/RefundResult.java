package com.safar.payment.gateway;

public record RefundResult(String refundId, long amountPaise, String status) {}
