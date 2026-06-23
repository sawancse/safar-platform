package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailContextBuilder;
import com.safar.notification.service.EmailGateway;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Consumes insurance policy lifecycle events: sends the certificate email +
 * WhatsApp + in-app on policy issued, and an in-app on cancel. The email links
 * to the self-hosted certificate ({base}/api/v1/insurance/certificate/{ref}),
 * which redirects to the insurer's PDF when a real partner is live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceEventConsumer {

    private final WhatsAppService whatsAppService;
    private final InAppNotificationService inAppNotificationService;
    private final EmailTemplateService emailTemplateService;
    private final EmailGateway emailGateway;
    private final ObjectMapper objectMapper;

    @Value("${notification.insurance-cert-base-url:https://api.bhramankaro.com/api/v1/insurance/certificate}")
    private String certBaseUrl;

    @Value("${notification.insurance-ops-email:support@bhramankaro.com}")
    private String opsEmail;

    @Value("${notification.base-url:https://bhramankaro.com}")
    private String webBaseUrl;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @KafkaListener(
            topics = {"insurance.policy.issued", "insurance.policy.cancelled"},
            groupId = "notification-insurance-group"
    )
    public void onInsuranceEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);
            switch (topic) {
                case "insurance.policy.issued" -> handleIssued(node);
                case "insurance.policy.cancelled" -> handleCancelled(node);
                default -> log.warn("Unhandled insurance topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Insurance event handling error on {}: {}", topic, e.getMessage(), e);
        }
    }

    private void handleIssued(JsonNode node) {
        String userId = node.path("userId").asText("");
        String phone = node.path("contactPhone").asText("");
        String email = node.path("contactEmail").asText("");
        String policyRef = node.path("policyRef").asText("");
        String provider = node.path("provider").asText("");
        String coverage = node.path("coverageType").asText("");
        // Always link to our self-hosted certificate (redirects to the insurer PDF when real).
        String certUrl = certBaseUrl + "/" + policyRef;

        // Certificate email — makes the "Certificate emailed to you" promise real.
        if (!email.isBlank()) {
            try {
                EmailContext ctx = new EmailContext();
                ctx.setGuestName(displayName(email));
                ctx.setGuestEmail(email);
                ctx.setInsurancePolicyRef(policyRef);
                ctx.setInsurancePolicyNumber(node.path("externalPolicyId").asText(""));
                ctx.setInsuranceProvider(prettyProvider(provider));
                ctx.setInsuranceCoverage(coverageLabel(coverage));
                ctx.setInsurancePremium(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.path("premiumPaise").asLong(0)));
                long si = node.path("sumInsuredPaise").asLong(0);
                if (si > 0) ctx.setInsuranceSumInsured(EmailContextBuilder.formatPaiseToRupeesWithSymbol(si));
                ctx.setInsuredCount(node.path("insuredCount").asInt(1));
                ctx.setInsuranceValidFrom(fmtDate(node.path("tripStartDate").asText("")));
                ctx.setInsuranceValidTo(fmtDate(node.path("tripEndDate").asText("")));
                ctx.setCertificateUrl(certUrl);
                emailTemplateService.sendHtmlEmail(email,
                        "Your insurance is active — " + policyRef,
                        "insurance-policy-issued", ctx);
            } catch (Exception e) {
                log.warn("Insurance-issued email failed for {}: {}", policyRef, e.getMessage());
            }
        }

        if (!phone.isBlank()) {
            try {
                whatsAppService.sendInsurancePolicyIssued(phone, policyRef, provider, coverage, certUrl);
            } catch (Exception e) {
                log.warn("WA insurance-issued failed: {}", e.getMessage());
            }
        }

        if (!userId.isBlank()) {
            try {
                inAppNotificationService.create(UUID.fromString(userId),
                        "Insurance issued",
                        "Your travel insurance " + policyRef + " is active. Tap to view your certificate.",
                        "INSURANCE_ISSUED", node.path("policyId").asText(""), "INSURANCE_POLICY");
            } catch (Exception e) {
                log.warn("In-app insurance-issued failed: {}", e.getMessage());
            }
        }
    }

    private void handleCancelled(JsonNode node) {
        String userId = node.path("userId").asText("");
        String policyRef = node.path("policyRef").asText("");

        if (!userId.isBlank()) {
            try {
                inAppNotificationService.create(UUID.fromString(userId),
                        "Insurance cancelled",
                        "Your travel insurance " + policyRef + " has been cancelled. Refund (if eligible) will be processed in 5-7 days.",
                        "INSURANCE_CANCELLED", node.path("policyId").asText(""), "INSURANCE_POLICY");
            } catch (Exception e) {
                log.warn("In-app insurance-cancelled failed: {}", e.getMessage());
            }
        }
    }

    // ── Advisor-callback lead → notify ops ──────────────────────────────────────
    @KafkaListener(topics = "insurance.lead.created", groupId = "notification-insurance-group")
    public void onLeadCreated(String message) {
        try {
            JsonNode n = objectMapper.readTree(message);
            String body = "New insurance advisor callback request:\n\n"
                    + "Name: " + n.path("name").asText("") + "\n"
                    + "Phone: " + n.path("phone").asText("") + "\n"
                    + "Email: " + n.path("email").asText("") + "\n"
                    + "Product: " + n.path("product").asText("") + "\n"
                    + "City: " + n.path("city").asText("") + "\n"
                    + "Preferred time: " + n.path("preferredTime").asText("") + "\n\n"
                    + "Open the admin leads queue to action this.";
            emailGateway.send(opsEmail, "New insurance lead — " + n.path("product").asText("call"), body);
        } catch (Exception e) {
            log.error("Insurance lead notify failed: {}", e.getMessage());
        }
    }

    // ── Renewal reminder → notify customer ──────────────────────────────────────
    @KafkaListener(topics = "insurance.policy.renewal-due", groupId = "notification-insurance-group")
    public void onRenewalDue(String message) {
        try {
            JsonNode n = objectMapper.readTree(message);
            String email = n.path("contactEmail").asText("");
            String policyRef = n.path("policyRef").asText("");
            int days = n.path("daysToExpiry").asInt(0);
            String userId = n.path("userId").asText("");
            if (!email.isBlank()) {
                String body = "Hi,\n\nYour " + coverageLabel(n.path("coverageType").asText("")) + " policy "
                        + policyRef + " expires in " + days + " days (" + fmtDate(n.path("expiryDate").asText("")) + ").\n\n"
                        + "Renew now to stay covered: " + webBaseUrl + "/services/insurance\n\n"
                        + "BhramanKaro Team";
                emailGateway.send(email, "Renew your insurance — " + policyRef + " expires in " + days + " days", body);
            }
            if (!userId.isBlank()) {
                try {
                    inAppNotificationService.create(UUID.fromString(userId),
                            "Insurance renewal due",
                            "Policy " + policyRef + " expires in " + days + " days. Tap to renew.",
                            "INSURANCE_RENEWAL_DUE", n.path("policyId").asText(""), "INSURANCE_POLICY");
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            log.error("Insurance renewal notify failed: {}", e.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String displayName(String email) {
        if (email == null || email.isBlank()) return "there";
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        local = local.replace('.', ' ').replace('_', ' ').trim();
        if (local.isEmpty()) return "there";
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }

    private static String prettyProvider(String p) {
        if (p == null || p.isBlank()) return "BhramanKaro Partner";
        return switch (p) {
            case "SANDBOX" -> "BhramanKaro Assured";
            case "AGGREGATOR" -> "BhramanKaro Insurance Partner";
            case "ACKO" -> "Acko";
            case "ICICI_LOMBARD" -> "ICICI Lombard";
            case "RELIANCE_GENERAL" -> "Reliance General";
            case "HDFC_ERGO" -> "HDFC ERGO";
            default -> p;
        };
    }

    private static String coverageLabel(String enumName) {
        if (enumName == null || enumName.isBlank()) return "Insurance";
        return switch (enumName) {
            case "INTERNATIONAL_TRAVEL" -> "International travel insurance";
            case "DOMESTIC_TRAVEL" -> "Domestic travel insurance";
            case "STUDENT_TRAVEL" -> "Student travel insurance";
            case "STAY_PROTECTION" -> "Stay protection";
            case "TENANT_CONTENTS" -> "Renter / tenant cover";
            case "HEALTH" -> "Health insurance";
            case "LIFE_TERM" -> "Term life insurance";
            case "MOTOR" -> "Motor insurance";
            case "HOME" -> "Home insurance";
            case "PERSONAL_ACCIDENT" -> "Personal accident cover";
            default -> {
                String s = enumName.replace('_', ' ').toLowerCase();
                yield Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        };
    }

    private static String fmtDate(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return LocalDate.parse(iso.length() > 10 ? iso.substring(0, 10) : iso).format(D);
        } catch (Exception e) {
            return iso;
        }
    }
}
