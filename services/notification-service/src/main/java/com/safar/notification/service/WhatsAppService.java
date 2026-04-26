package com.safar.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WhatsApp Business API delivery via MSG91's WA channel.
 *
 * MSG91 WA uses pre-approved Meta template IDs (regulatory requirement —
 * Meta Business Manager must approve every template before it can be sent).
 * Until a template is approved, the corresponding template-id env var
 * stays blank and the call is a no-op (logged at debug level).
 *
 * Docs: https://docs.msg91.com/whatsapp/integration-guide
 */
@Service
@Slf4j
public class WhatsAppService {

    @Value("${msg91.auth-key:}")
    private String authKey;

    @Value("${msg91.wa.integrated-number:}")
    private String integratedNumber;       // Meta-verified Safar WA Business number

    @Value("${msg91.wa.namespace:}")
    private String namespace;              // Meta WA template namespace

    @Value("${msg91.wa.flight-search-abandoned-template:}")
    private String flightSearchAbandonedTemplate;

    // WA BD — book/manage flights + insurance via WhatsApp commerce
    @Value("${msg91.wa.flight-booking-confirmed-template:}")
    private String flightBookingConfirmedTemplate;

    @Value("${msg91.wa.insurance-policy-issued-template:}")
    private String insurancePolicyIssuedTemplate;

    @Value("${msg91.wa.book-flight-deeplink-template:}")
    private String bookFlightDeeplinkTemplate;

    @Value("${msg91.wa.book-insurance-deeplink-template:}")
    private String bookInsuranceDeeplinkTemplate;

    @Value("${msg91.wa.bot-help-template:}")
    private String botHelpTemplate;

    @Value("${msg91.base-url:https://control.msg91.com/api/v5}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Abandoned-search recovery WhatsApp message — fired by the
     * FlightSearchAbandonedConsumer on every pulse (1h, 6h, 24h).
     * WhatsApp has highest open rate (~70%) so it's the primary channel
     * for this recovery campaign in the Indian market.
     */
    public void sendFlightSearchAbandoned(String phone, String route, String date, String fareHint) {
        if (!isConfigured(flightSearchAbandonedTemplate)) return;
        sendTemplate(phone, flightSearchAbandonedTemplate, List.of(
                route, date, fareHint != null ? fareHint : ""));
        log.info("Sent flight search-abandoned WA to {}", maskPhone(phone));
    }

    /** E-ticket confirmation via WhatsApp — fired by FlightEventConsumer on flight.booking.confirmed. */
    public void sendFlightBookingConfirmed(String phone, String bookingRef, String route, String date, String airline) {
        if (!isConfigured(flightBookingConfirmedTemplate)) return;
        sendTemplate(phone, flightBookingConfirmedTemplate, List.of(
                bookingRef, route, date, airline != null ? airline : ""));
        log.info("Sent flight confirmation WA to {} ({})", maskPhone(phone), bookingRef);
    }

    /** Policy doc + cert URL via WhatsApp — fired on insurance.policy.issued. */
    public void sendInsurancePolicyIssued(String phone, String policyRef, String insurer, String coverage, String certUrl) {
        if (!isConfigured(insurancePolicyIssuedTemplate)) return;
        sendTemplate(phone, insurancePolicyIssuedTemplate, List.of(
                policyRef, insurer, coverage, certUrl != null ? certUrl : "ysafar.com/insurance"));
        log.info("Sent insurance policy WA to {} ({})", maskPhone(phone), policyRef);
    }

    /** Reply to inbound "BOOK FLIGHT" with a deep link back to safar-web. */
    public void sendBookFlightDeeplink(String phone, String deeplink) {
        if (!isConfigured(bookFlightDeeplinkTemplate)) return;
        sendTemplate(phone, bookFlightDeeplinkTemplate, List.of(deeplink));
        log.info("Sent book-flight deeplink WA to {}", maskPhone(phone));
    }

    /** Reply to inbound "BOOK INSURANCE" with a deep link back to safar-web. */
    public void sendBookInsuranceDeeplink(String phone, String deeplink) {
        if (!isConfigured(bookInsuranceDeeplinkTemplate)) return;
        sendTemplate(phone, bookInsuranceDeeplinkTemplate, List.of(deeplink));
        log.info("Sent book-insurance deeplink WA to {}", maskPhone(phone));
    }

    /** Catch-all reply for unknown inbound keywords — explains commands. */
    public void sendBotHelp(String phone) {
        if (!isConfigured(botHelpTemplate)) return;
        sendTemplate(phone, botHelpTemplate, List.of());
        log.info("Sent bot-help WA to {}", maskPhone(phone));
    }

    // ── Core WA sender ──────────────────────────────────────────

    /**
     * Sends a Meta-approved WhatsApp template via MSG91's WA endpoint.
     * Template parameters are positional (Meta's contract — {{1}}, {{2}}, ...).
     */
    private void sendTemplate(String phone, String templateName, List<String> params) {
        if (authKey == null || authKey.isBlank()
                || integratedNumber == null || integratedNumber.isBlank()) {
            log.warn("MSG91 WA not configured (auth-key or integrated-number missing), skipping");
            return;
        }
        try {
            String mobile = normalizePhone(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            // MSG91 WhatsApp payload shape — language defaults to en; switch to hi/regional
            // when localisation lookup is wired (per cross-vertical engine design EN+HI).
            Map<String, Object> components = new HashMap<>();
            components.put("type", "body");
            components.put("parameters", params.stream()
                    .map(p -> Map.of("type", "text", "text", p))
                    .toList());

            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);
            template.put("language", Map.of("policy", "deterministic", "code", "en"));
            template.put("namespace", namespace != null ? namespace : "");
            template.put("components", List.of(components));

            Map<String, Object> message = Map.of(
                    "to", List.of(mobile),
                    "type", "template",
                    "template", template
            );

            Map<String, Object> body = Map.of(
                    "integrated_number", integratedNumber,
                    "content_type", "template",
                    "payload", message
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/whatsapp/whatsapp-outbound-message/bulk/", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("MSG91 WA sent to {} template={}", maskPhone(phone), templateName);
            } else {
                log.error("MSG91 WA failed for {}: {} {}", maskPhone(phone),
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("MSG91 WA send failed for {}: {}", maskPhone(phone), e.getMessage());
        }
    }

    private boolean isConfigured(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            log.debug("WA template name not configured (Meta approval pending), skipping");
            return false;
        }
        return true;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        // MSG91 WhatsApp expects country-code prefixed digits (e.g. 919876543210), no '+'
        if (digits.length() == 10) return "91" + digits;
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
