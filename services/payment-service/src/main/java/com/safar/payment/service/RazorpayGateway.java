package com.safar.payment.service;

public interface RazorpayGateway {
    /**
     * Creates a Razorpay plan and subscription, returns the subscription ID.
     */
    String createSubscription(String tierName, long totalAmountPaise) throws Exception;

    /**
     * Creates a Razorpay order for booking payment, returns the order ID.
     */
    String createOrder(long amountPaise, String receipt) throws Exception;

    /**
     * Verifies HMAC-SHA256 webhook signature.
     */
    boolean verifyWebhookSignature(String payload, String signature, String secret);

    /**
     * Verifies Razorpay payment signature (order_id|payment_id signed with key secret).
     */
    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /**
     * Initiates a refund for a captured payment. Returns the gateway refund ID.
     */
    String refund(String razorpayPaymentId, long amountPaise) throws Exception;

    /**
     * Cancels a Razorpay subscription. cancelAtCycleEnd=true cancels at end of current billing period.
     */
    void cancelSubscription(String subscriptionId, boolean cancelAtCycleEnd) throws Exception;

    /**
     * Creates a Razorpay payout via Fund Account + Payout API (or direct transfer).
     * Returns the payout ID.
     */
    String createPayout(String upiId, long amountPaise, String purpose, String referenceId) throws Exception;
}
