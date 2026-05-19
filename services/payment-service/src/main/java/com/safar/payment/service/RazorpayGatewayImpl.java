package com.safar.payment.service;

import com.razorpay.Order;
import com.razorpay.PaymentLink;
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
    public void cancelSubscription(String subscriptionId, boolean cancelAtCycleEnd) throws Exception {
        JSONObject cancelReq = new JSONObject();
        cancelReq.put("cancel_at_cycle_end", (Object) (cancelAtCycleEnd ? 1 : 0));
        razorpay.subscriptions.cancel(subscriptionId, cancelReq);
        log.info("Razorpay subscription cancelled: {} (atCycleEnd={})", subscriptionId, cancelAtCycleEnd);
    }

    @Override
    public String createPayout(String upiId, long amountPaise, String purpose, String referenceId) throws Exception {
        // Razorpay Payouts API: create a contact + fund account + payout
        // For UPI payouts, we use the RazorpayX Payout API
        JSONObject payoutReq = new JSONObject();
        payoutReq.put("account_number", (Object) System.getenv("RAZORPAY_ACCOUNT_NUMBER"));
        payoutReq.put("amount", (Object) amountPaise);
        payoutReq.put("currency", (Object) "INR");
        payoutReq.put("mode", (Object) "UPI");
        payoutReq.put("purpose", (Object) purpose);
        payoutReq.put("reference_id", (Object) referenceId);

        JSONObject fundAccount = new JSONObject();
        JSONObject vpa = new JSONObject();
        vpa.put("address", (Object) upiId);
        fundAccount.put("account_type", (Object) "vpa");
        fundAccount.put("vpa", (Object) vpa);
        payoutReq.put("fund_account", (Object) fundAccount);

        // Note: Razorpay Payouts API uses RazorpayX (different base URL)
        // The razorpay-java SDK supports payouts via razorpay.payouts.create()
        // In sandbox/test mode, payouts are auto-processed
        log.info("Creating Razorpay payout: {} paise to UPI {} (ref={})", amountPaise, upiId, referenceId);

        // Razorpay Java SDK doesn't have a direct payouts.create() — use orders for now
        // and implement the actual RazorpayX call when going live.
        // For production: HTTP POST to https://api.razorpay.com/v1/payouts with above JSON
        log.warn("Razorpay Payouts API (RazorpayX) requires separate activation. " +
                "Payout recorded in DB — process manually or via RazorpayX dashboard until API is activated.");
        return "pout_pending_" + referenceId;
    }

    @Override
    public PaymentLinkResult createPaymentLink(
            long amountPaise,
            String description,
            String customerName,
            String customerPhone,
            String customerEmail,
            String bookingId
    ) throws Exception {
        JSONObject req = new JSONObject();
        req.put("amount", (Object) amountPaise);
        req.put("currency", (Object) "INR");
        req.put("accept_partial", (Object) false);
        req.put("description", (Object) (description != null ? description : "Safar booking balance"));
        // Razorpay auto-sends WhatsApp + SMS + email when these flags are true.
        req.put("reference_id", (Object) ("bal-" + bookingId + "-" + System.currentTimeMillis()));

        JSONObject customer = new JSONObject();
        if (customerName != null && !customerName.isBlank()) customer.put("name", (Object) customerName);
        if (customerPhone != null && !customerPhone.isBlank()) customer.put("contact", (Object) customerPhone);
        if (customerEmail != null && !customerEmail.isBlank()) customer.put("email", (Object) customerEmail);
        req.put("customer", (Object) customer);

        JSONObject notify = new JSONObject();
        notify.put("sms", (Object) (customerPhone != null && !customerPhone.isBlank()));
        notify.put("email", (Object) (customerEmail != null && !customerEmail.isBlank()));
        req.put("notify", (Object) notify);

        JSONObject notes = new JSONObject();
        notes.put("bookingId", (Object) bookingId);
        notes.put("purpose", (Object) "BALANCE_PAYMENT");
        req.put("notes", (Object) notes);

        PaymentLink link = razorpay.paymentLink.create(req);
        String linkId = link.get("id").toString();
        String shortUrl = link.get("short_url").toString();
        log.info("Razorpay PaymentLink created for booking {}: {} -> {}", bookingId, linkId, shortUrl);
        return new PaymentLinkResult(linkId, shortUrl);
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
