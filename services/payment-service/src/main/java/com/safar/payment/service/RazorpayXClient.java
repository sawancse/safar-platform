package com.safar.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Minimal RazorpayX Payouts client (contacts → fund accounts → payouts).
 * razorpay-java has no payouts API, so this talks to the REST API directly.
 * Fails closed: never returns a made-up id; any misconfiguration or non-2xx throws.
 * See docs/design-host-payout.md §4.3.
 */
@Component
@Slf4j
public class RazorpayXClient {

    private static final String BASE = "https://api.razorpay.com/v1";
    /** IMPS limit is ₹5,00,000; above it NEFT is used. */
    private static final long IMPS_MAX_PAISE = 500_000_00L;

    private final String keyId;
    private final String keySecret;
    private final String accountNumber;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public RazorpayXClient(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpayx.account-number:}") String accountNumber) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.accountNumber = accountNumber;
    }

    public record PayoutResult(String payoutId, String status, String fundAccountId, String utr) {}

    public boolean isConfigured() {
        return accountNumber != null && !accountNumber.isBlank()
                && keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String createContact(String name, String email, String referenceId) throws Exception {
        JSONObject req = new JSONObject();
        req.put("name", (Object) name);
        if (email != null && !email.isBlank()) req.put("email", (Object) email);
        req.put("type", (Object) "vendor");
        req.put("reference_id", (Object) trunc(referenceId, 40));
        return post("/contacts", req, null).getString("id");
    }

    public String createBankFundAccount(String contactId, String name, String ifsc, String accountNo) throws Exception {
        JSONObject bank = new JSONObject();
        bank.put("name", (Object) name);
        bank.put("ifsc", (Object) ifsc);
        bank.put("account_number", (Object) accountNo);
        JSONObject req = new JSONObject();
        req.put("contact_id", (Object) contactId);
        req.put("account_type", (Object) "bank_account");
        req.put("bank_account", (Object) bank);
        return post("/fund_accounts", req, null).getString("id");
    }

    public String createVpaFundAccount(String contactId, String upiId) throws Exception {
        JSONObject vpa = new JSONObject();
        vpa.put("address", (Object) upiId);
        JSONObject req = new JSONObject();
        req.put("contact_id", (Object) contactId);
        req.put("account_type", (Object) "vpa");
        req.put("vpa", (Object) vpa);
        return post("/fund_accounts", req, null).getString("id");
    }

    /**
     * Creates a payout. {@code idempotencyKey} is sent as X-Payout-Idempotency so a retried
     * HTTP call can never double-pay. Insufficient balance fails (no silent queueing).
     */
    public PayoutResult createPayout(String fundAccountId, boolean vpa, long amountPaise, String purpose,
                                     String referenceId, String idempotencyKey) throws Exception {
        if (amountPaise <= 0) throw new IllegalArgumentException("Payout amount must be positive");
        JSONObject req = new JSONObject();
        req.put("account_number", (Object) accountNumber);
        req.put("fund_account_id", (Object) fundAccountId);
        req.put("amount", (Object) amountPaise);
        req.put("currency", (Object) "INR");
        req.put("mode", (Object) (vpa ? "UPI" : amountPaise <= IMPS_MAX_PAISE ? "IMPS" : "NEFT"));
        req.put("purpose", (Object) purpose);
        req.put("queue_if_low_balance", (Object) false);
        req.put("reference_id", (Object) trunc(referenceId, 40));
        JSONObject res = post("/payouts", req, idempotencyKey);
        return new PayoutResult(res.getString("id"), res.optString("status", ""), fundAccountId,
                res.isNull("utr") ? null : res.optString("utr", null));
    }

    static String trunc(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max);
    }

    private JSONObject post(String path, JSONObject body, String idempotencyKey) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("RazorpayX not configured (razorpayx.account-number / Razorpay keys missing) — payout not sent");
        }
        String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/json");
        if (idempotencyKey != null) b.header("X-Payout-Idempotency", idempotencyKey);
        HttpResponse<String> resp = http.send(
                b.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
        JSONObject json;
        try {
            json = new JSONObject(resp.body());
        } catch (Exception e) {
            throw new IllegalStateException("RazorpayX " + path + " returned non-JSON (HTTP " + resp.statusCode() + ")");
        }
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            JSONObject err = json.optJSONObject("error");
            String msg = err != null ? err.optString("description", "HTTP " + resp.statusCode()) : "HTTP " + resp.statusCode();
            log.warn("RazorpayX {} failed: {}", path, msg);
            throw new IllegalStateException("RazorpayX " + path + ": " + msg);
        }
        return json;
    }
}
