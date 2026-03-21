package com.safar.payment.gateway;

public interface PaymentGateway {
    String name();
    OrderResult createOrder(long amountPaise, String currency, String receipt, String description);
    CaptureResult capturePayment(String gatewayOrderId, String gatewayPaymentId, String signature);
    RefundResult refund(String gatewayPaymentId, long amountPaise, String reason);
    boolean verifyWebhook(String payload, String signature, String secret);
    SubscriptionResult createSubscription(String planName, long amountPaise, String currency);
}
