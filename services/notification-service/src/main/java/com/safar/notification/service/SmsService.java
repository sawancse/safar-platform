package com.safar.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Transactional SMS via MSG91 Flow API.
 * Used for rent reminders, payment confirmations, and other non-OTP notifications.
 * Docs: https://docs.msg91.com/reference/send-sms
 */
@Service
@Slf4j
public class SmsService {

    @Value("${msg91.auth-key:}")
    private String authKey;

    @Value("${msg91.sender-id:SAFAR}")
    private String senderId;

    @Value("${msg91.sms.rent-reminder-template-id:}")
    private String rentReminderTemplateId;

    @Value("${msg91.sms.rent-urgent-template-id:}")
    private String rentUrgentTemplateId;

    @Value("${msg91.sms.rent-advance-template-id:}")
    private String rentAdvanceTemplateId;

    @Value("${msg91.sms.rent-overdue-template-id:}")
    private String rentOverdueTemplateId;

    @Value("${msg91.sms.invoice-template-id:}")
    private String invoiceTemplateId;

    @Value("${msg91.base-url:https://control.msg91.com/api/v5}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send transactional SMS for PG rent advance reminder (7 days before billing).
     */
    public void sendRentAdvanceReminder(String phone, String tenantName, String amount, String billingDate) {
        if (!isConfigured(rentAdvanceTemplateId)) return;
        Map<String, String> vars = Map.of(
                "name", tenantName,
                "amount", amount,
                "date", billingDate
        );
        sendTemplateSms(phone, rentAdvanceTemplateId, vars);
        log.info("Sent rent advance SMS to {}", maskPhone(phone));
    }

    /**
     * Send transactional SMS for rent reminder (5 days before due).
     */
    public void sendRentReminder(String phone, String tenantName, String amount, String dueDate, String invoiceNumber) {
        if (!isConfigured(rentReminderTemplateId)) return;
        Map<String, String> vars = Map.of(
                "name", tenantName,
                "amount", amount,
                "date", dueDate,
                "invoice", invoiceNumber
        );
        sendTemplateSms(phone, rentReminderTemplateId, vars);
        log.info("Sent rent reminder SMS to {}", maskPhone(phone));
    }

    /**
     * Send transactional SMS for urgent rent reminder (1 day before due).
     */
    public void sendRentUrgentReminder(String phone, String tenantName, String amount, String dueDate) {
        if (!isConfigured(rentUrgentTemplateId)) return;
        Map<String, String> vars = Map.of(
                "name", tenantName,
                "amount", amount,
                "date", dueDate
        );
        sendTemplateSms(phone, rentUrgentTemplateId, vars);
        log.info("Sent rent urgent SMS to {}", maskPhone(phone));
    }

    /**
     * Send transactional SMS for overdue invoice.
     */
    public void sendRentOverdue(String phone, String tenantName, String amount, String penalty) {
        if (!isConfigured(rentOverdueTemplateId)) return;
        Map<String, String> vars = Map.of(
                "name", tenantName,
                "amount", amount,
                "penalty", penalty
        );
        sendTemplateSms(phone, rentOverdueTemplateId, vars);
        log.info("Sent rent overdue SMS to {}", maskPhone(phone));
    }

    /**
     * Send transactional SMS for new invoice generated.
     */
    public void sendInvoiceGenerated(String phone, String tenantName, String amount, String dueDate, String invoiceNumber) {
        if (!isConfigured(invoiceTemplateId)) return;
        Map<String, String> vars = Map.of(
                "name", tenantName,
                "amount", amount,
                "date", dueDate,
                "invoice", invoiceNumber
        );
        sendTemplateSms(phone, invoiceTemplateId, vars);
        log.info("Sent invoice SMS to {}", maskPhone(phone));
    }

    // ── Flight SMS ─────────────────────────────────────────────

    @Value("${msg91.sms.flight-confirmed-template-id:}")
    private String flightConfirmedTemplateId;

    @Value("${msg91.sms.flight-cancelled-template-id:}")
    private String flightCancelledTemplateId;

    @Value("${msg91.sms.flight-checkin-template-id:}")
    private String flightCheckinTemplateId;

    public void sendFlightConfirmation(String phone, String bookingRef, String route, String date, String flight) {
        if (!isConfigured(flightConfirmedTemplateId)) return;
        sendTemplateSms(phone, flightConfirmedTemplateId, Map.of(
                "ref", bookingRef, "route", route, "date", date, "flight", flight));
        log.info("Sent flight confirmation SMS to {}", maskPhone(phone));
    }

    public void sendFlightCancellation(String phone, String bookingRef, String route, String refund) {
        if (!isConfigured(flightCancelledTemplateId)) return;
        sendTemplateSms(phone, flightCancelledTemplateId, Map.of(
                "ref", bookingRef, "route", route, "refund", refund));
        log.info("Sent flight cancellation SMS to {}", maskPhone(phone));
    }

    public void sendFlightCheckinReminder(String phone, String bookingRef, String route, String flight) {
        if (!isConfigured(flightCheckinTemplateId)) return;
        sendTemplateSms(phone, flightCheckinTemplateId, Map.of(
                "ref", bookingRef, "route", route, "flight", flight));
        log.info("Sent flight check-in reminder SMS to {}", maskPhone(phone));
    }

    // ── Core SMS sender ─────────────────────────────────────────

    private void sendTemplateSms(String phone, String templateId, Map<String, String> variables) {
        if (authKey == null || authKey.isBlank()) {
            log.warn("MSG91 auth key not configured, skipping SMS");
            return;
        }

        try {
            String mobile = normalizePhone(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            // MSG91 Flow API format
            Map<String, Object> recipient = new java.util.HashMap<>(variables);
            recipient.put("mobiles", mobile);

            Map<String, Object> body = Map.of(
                    "template_id", templateId,
                    "sender", senderId,
                    "short_url", "0",
                    "recipients", List.of(recipient)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/flow/", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("MSG91 SMS sent to {} template={}", maskPhone(phone), templateId);
            } else {
                log.error("MSG91 SMS failed for {}: {} {}", maskPhone(phone),
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("MSG91 SMS send failed for {}: {}", maskPhone(phone), e.getMessage());
        }
    }

    private boolean isConfigured(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            log.debug("SMS template ID not configured, skipping");
            return false;
        }
        return true;
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) return "91" + digits;
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
