package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DonationEventConsumer {

    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "donation.captured", groupId = "notification-donation-group")
    public void onDonationCaptured(String message) {
        try {
            // message is the donation UUID — fetch donation details via REST or parse JSON
            JsonNode node = tryParseJson(message);

            EmailContext ctx = new EmailContext();

            if (node != null) {
                ctx.setDonationRef(node.has("donationRef") ? node.get("donationRef").asText() : "");
                ctx.setDonorName(node.has("donorName") ? node.get("donorName").asText() : "Generous Donor");
                ctx.setDonorEmail(node.has("donorEmail") ? node.get("donorEmail").asText() : "");
                ctx.setDonationAmount(node.has("amountPaise")
                    ? formatPaise(node.get("amountPaise").asLong()) : "");
                ctx.setDonationFrequency(node.has("frequency") ? node.get("frequency").asText() : "ONE_TIME");
                ctx.setReceiptNumber(node.has("receiptNumber") ? node.get("receiptNumber").asText() : "");
                ctx.setDonorPan(node.has("donorPan") ? node.get("donorPan").asText() : "");
                ctx.setDedicatedTo(node.has("dedicatedTo") ? node.get("dedicatedTo").asText() : "");
                ctx.setDonationTier(determineTier(node.has("amountPaise") ? node.get("amountPaise").asLong() : 0));
                ctx.setTaxSaving(node.has("amountPaise")
                    ? formatPaise(node.get("amountPaise").asLong() / 2) : "");

                String email = ctx.getDonorEmail();
                if (email != null && !email.isBlank()) {
                    String subject = "Thank you for your Aashray donation - 80G Receipt " + ctx.getReceiptNumber();
                    emailTemplateService.sendHtmlEmail(email, subject, "donation-receipt", ctx);
                    log.info("Donation receipt sent to {} for {}", email, ctx.getDonationRef());
                } else {
                    log.info("Donation {} captured but no email provided - skipping receipt", ctx.getDonationRef());
                }
            } else {
                // Plain string message (donation ID) — can't send email without details
                log.info("Donation captured event received: {} (no email context)", message);
            }
        } catch (Exception e) {
            log.error("Error processing donation.captured event: {}", e.getMessage(), e);
        }
    }

    private JsonNode tryParseJson(String message) {
        if (message == null || !message.trim().startsWith("{")) return null;
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatPaise(long paise) {
        long rupees = paise / 100;
        return "\u20B9" + String.format("%,d", rupees);
    }

    private String determineTier(long amountPaise) {
        long rupees = amountPaise / 100;
        if (rupees >= 15000) return "Shelter Patron";
        if (rupees >= 5000) return "Shelter Champion";
        if (rupees >= 2000) return "Shelter Builder";
        if (rupees >= 500) return "Shelter Friend";
        return "Supporter";
    }
}
