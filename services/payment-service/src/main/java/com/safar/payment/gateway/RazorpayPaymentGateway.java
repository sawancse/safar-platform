package com.safar.payment.gateway;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("razorpayGateway")
@Slf4j
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentGateway(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public String name() {
        return "razorpay";
    }

    @Override
    public OrderResult createOrder(long amountPaise, String currency, String receipt, String description) {
        try {
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", (Object) amountPaise);
            orderReq.put("currency", (Object) currency);
            orderReq.put("receipt", (Object) receipt);
            if (description != null && !description.isEmpty()) {
                JSONObject notes = new JSONObject();
                notes.put("description", (Object) description);
                orderReq.put("notes", (Object) notes);
            }
            Order order = razorpayClient.orders.create(orderReq);
            String orderId = order.get("id").toString();
            log.info("Razorpay order created: {} for {} {}", orderId, amountPaise, currency);
            return new OrderResult(orderId, amountPaise, currency, keyId);
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public CaptureResult capturePayment(String gatewayOrderId, String gatewayPaymentId, String signature) {
        try {
            JSONObject attrs = new JSONObject();
            attrs.put("razorpay_order_id", (Object) gatewayOrderId);
            attrs.put("razorpay_payment_id", (Object) gatewayPaymentId);
            attrs.put("razorpay_signature", (Object) signature);
            Utils.verifyPaymentSignature(attrs, keySecret);
            return new CaptureResult(gatewayPaymentId, null, true);
        } catch (Exception e) {
            log.warn("Payment signature verification failed: {}", e.getMessage());
            return new CaptureResult(gatewayPaymentId, null, false);
        }
    }

    @Override
    public RefundResult refund(String gatewayPaymentId, long amountPaise, String reason) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", (Object) amountPaise);
            request.put("notes", (Object) new JSONObject().put("reason", (Object) reason));
            com.razorpay.Refund refund = razorpayClient.payments.refund(gatewayPaymentId, request);
            return new RefundResult(refund.get("id"), amountPaise, "processed");
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhook(String payload, String signature, String secret) {
        try {
            Utils.verifyWebhookSignature(payload, signature, secret);
            return true;
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public SubscriptionResult createSubscription(String planName, long amountPaise, String currency) {
        try {
            JSONObject item = new JSONObject();
            item.put("name", (Object) ("Safar " + planName + " Plan"));
            item.put("amount", (Object) amountPaise);
            item.put("currency", (Object) currency);

            JSONObject planReq = new JSONObject();
            planReq.put("period", (Object) "monthly");
            planReq.put("interval", (Object) 1);
            planReq.put("item", (Object) item);
            Plan plan = razorpayClient.plans.create(planReq);

            String planId = plan.get("id").toString();
            JSONObject subReq = new JSONObject();
            subReq.put("plan_id", (Object) planId);
            subReq.put("total_count", (Object) 120);
            Subscription sub = razorpayClient.subscriptions.create(subReq);

            String subscriptionId = sub.get("id").toString();
            log.info("Razorpay subscription created: {} for plan {}", subscriptionId, planId);
            return new SubscriptionResult(subscriptionId, planId, "created");
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay subscription creation failed: " + e.getMessage(), e);
        }
    }
}
