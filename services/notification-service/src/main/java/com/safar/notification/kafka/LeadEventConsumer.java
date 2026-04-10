package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailGateway;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Handles lead lifecycle Kafka events — welcome emails, nurture drips,
 * price drop alerts, re-engagement campaigns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadEventConsumer {

    private final EmailGateway emailGateway;
    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"lead.captured", "lead.nurture.welcome", "lead.nurture.day3",
                      "lead.nurture.day7", "lead.nurture.re-engagement",
                      "lead.price.dropped", "lead.locality.new-listing"},
            groupId = "notification-lead-group"
    )
    public void onLeadEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String email = txt(node, "email");
            String name = txt(node, "name");
            String city = txt(node, "city");
            String source = txt(node, "source");
            String leadType = txt(node, "leadType");
            String subject = txt(node, "subject");

            if (email == null || email.isBlank()) {
                log.warn("Lead event on {} has no email, skipping", topic);
                return;
            }

            EmailContext ctx = new EmailContext();
            ctx.setGuestName(name != null && !name.isBlank() ? name : "Explorer");
            ctx.setCity(city);

            switch (topic) {
                case "lead.captured" -> sendLeadEmail(email, "Welcome to Safar — Your journey begins here!", "lead-welcome", ctx);
                case "lead.nurture.welcome" -> sendLeadEmail(email, subject != null ? subject : "Welcome to Safar!", "lead-welcome", ctx);
                case "lead.nurture.day3" -> sendLeadEmail(email, subject != null ? subject : "Top stays in " + (city != null ? city : "India"), "lead-day3-deals", ctx);
                case "lead.nurture.day7" -> sendLeadEmail(email, subject != null ? subject : "₹500 off your first stay!", "lead-day7-offer", ctx);
                case "lead.nurture.re-engagement" -> sendLeadEmail(email, subject != null ? subject : "We miss you! New stays added", "lead-re-engagement", ctx);
                case "lead.price.dropped" -> {
                    ctx.setListingTitle(txt(node, "listingTitle"));
                    String newPrice = node.has("newPricePaise") ? formatINR(node.get("newPricePaise").asLong()) : "";
                    ctx.setTotalAmount(newPrice);
                    sendLeadEmail(email, "Price dropped on " + txt(node, "listingTitle") + "!", "lead-price-drop", ctx);
                }
                case "lead.locality.new-listing" -> {
                    ctx.setListingTitle(txt(node, "listingTitle"));
                    ctx.setListingCity(city);
                    sendLeadEmail(email, "New listing in " + (city != null ? city : "your area") + "!", "lead-new-listing", ctx);
                }
                default -> log.warn("Unhandled lead topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling lead event on {}: {}", topic, e.getMessage());
        }
    }

    private void sendLeadEmail(String email, String subject, String templateName, EmailContext ctx) {
        try {
            emailTemplateService.sendHtmlEmail(email, subject, templateName, ctx);
            log.debug("Lead email sent: {} → {} ({})", templateName, email, subject);
        } catch (Exception e) {
            // Fallback to plain text
            try {
                emailGateway.send(email, subject,
                        "Hi " + (ctx.getGuestName() != null ? ctx.getGuestName() : "") + ",\n\n"
                        + subject + "\n\n"
                        + "Explore stays: https://ysafar.com\n\n"
                        + "Safar Team");
            } catch (Exception e2) {
                log.error("Lead email failed for {}: {}", email, e2.getMessage());
            }
        }
    }

    private String txt(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String formatINR(long paise) {
        return "₹" + String.format("%,d", paise / 100);
    }
}
