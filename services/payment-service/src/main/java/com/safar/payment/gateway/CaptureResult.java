package com.safar.payment.gateway;

public record CaptureResult(String gatewayPaymentId, String method, boolean verified) {}
