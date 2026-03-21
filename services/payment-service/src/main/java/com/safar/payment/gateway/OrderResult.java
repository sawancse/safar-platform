package com.safar.payment.gateway;

public record OrderResult(String gatewayOrderId, long amountPaise, String currency, String gatewayKeyId) {}
