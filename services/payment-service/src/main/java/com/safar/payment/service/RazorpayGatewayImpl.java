package com.safar.payment.service;

import com.razorpay.Order;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RazorpayGatewayImpl implements RazorpayGateway {

    private final RazorpayClient razorpay;
    private final String keySecret;

    public RazorpayGatewayImpl(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret) throws Exception {
        this.razorpay = new RazorpayClient(keyId, keySecret);
        this.keySecret = keySecret;
    }

    @Override
    public String createSubscription(String tierName, long totalAmountPaise) throws Exception {
        JSONObject item = new JSONObject();
        item.put("name", (Object) ("Safar " + tierName + " Plan"));
        item.put("amount", (Object) totalAmountPaise);
        item.put("currency", (Object) "INR");

        JSONObject planReq = new JSONObject();
        planReq.put("period", (Object) "monthly");
        planReq.put("interval", (Object) 1);
        planReq.put("item", (Object) item);
        Plan plan = razorpay.plans.create(planReq);

        String planId = plan.get("id").toString();
        JSONObject subReq = new JSONObject();
        subReq.put("plan_id", (Object) planId);
        subReq.put("total_count", (Object) 120);
        Subscription sub = razorpay.subscriptions.create(subReq);
        return sub.get("id").toString();
    }

    @Override
    public String createOrder(long amountPaise, String receipt) throws Exception {
        JSONObject orderReq = new JSONObject();
        orderReq.put("amount", (Object) amountPaise);
        orderReq.put("currency", (Object) "INR");
        orderReq.put("receipt", (Object) receipt);
        Order order = razorpay.orders.create(orderReq);
        return order.get("id").toString();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            Utils.verifyWebhookSignature(payload, signature, secret);
            return true;
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String refund(String razorpayPaymentId, long amountPaise) throws Exception {
        JSONObject refundReq = new JSONObject();
        refundReq.put("amount", (Object) amountPaise);
        refundReq.put("speed", (Object) "normal");
        com.razorpay.Refund refund = razorpay.payments.refund(razorpayPaymentId, refundReq);
        return refund.get("id").toString();
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attrs = new JSONObject();
            attrs.put("razorpay_order_id", (Object) orderId);
            attrs.put("razorpay_payment_id", (Object) paymentId);
            attrs.put("razorpay_signature", (Object) signature);
            Utils.verifyPaymentSignature(attrs, keySecret);
            return true;
        } catch (Exception e) {
            log.warn("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
