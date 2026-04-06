package com.safar.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * MSG91 OTP API client.
 * Docs: https://docs.msg91.com/reference/send-otp
 */
@Slf4j
@Component
public class Msg91Client {

    @Value("${msg91.auth-key:}")
    private String authKey;

    @Value("${msg91.template-id:}")
    private String templateId;

    @Value("${msg91.sender-id:SAFAR}")
    private String senderId;

    @Value("${msg91.otp-length:6}")
    private int otpLength;

    @Value("${msg91.otp-expiry:10}")
    private int otpExpiry;

    @Value("${msg91.base-url:https://control.msg91.com/api/v5}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send OTP via MSG91's default OTP API.
     * MSG91 generates the OTP and sends it using the default template.
     * We store the OTP in Redis ourselves for verification.
     *
     * @param phone Phone number (10 digits or with 91 prefix)
     * @param otp   Our generated OTP — passed via the `otp` param so MSG91 sends this exact code
     */
    public boolean sendOtp(String phone, String otp) {
        if (authKey == null || authKey.isBlank()) {
            log.warn("MSG91 auth key not configured, skipping SMS");
            return false;
        }

        try {
            String mobile = normalizePhone(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            // Use MSG91 OTP API with our own OTP value
            // Passing "otp" field sends this exact code instead of MSG91-generated one
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("mobile", mobile);
            body.put("otp", otp);
            body.put("otp_length", otpLength);
            body.put("otp_expiry", otpExpiry);
            body.put("sender", senderId);
            if (templateId != null && !templateId.isBlank()) {
                body.put("template_id", templateId);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/otp", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("MSG91 OTP sent to {}", maskPhone(phone));
                return true;
            } else {
                log.error("MSG91 OTP failed for {}: {} {}", maskPhone(phone),
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("MSG91 OTP send failed for {}: {}", maskPhone(phone), e.getMessage());
            return false;
        }
    }

    /**
     * Retry/resend OTP via MSG91 retry API.
     */
    public boolean retryOtp(String phone, String retryType) {
        try {
            String mobile = normalizePhone(phone);
            String url = baseUrl + "/otp/retry?mobile=" + mobile + "&retrytype=" + retryType;

            HttpHeaders headers = new HttpHeaders();
            headers.set("authkey", authKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("MSG91 retry failed for {}: {}", maskPhone(phone), e.getMessage());
            return false;
        }
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        // Ensure 91 country code prefix
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
