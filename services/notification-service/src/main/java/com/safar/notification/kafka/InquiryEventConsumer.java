package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.service.EmailGateway;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class InquiryEventConsumer {

    private final ObjectMapper objectMapper;
    private final EmailGateway emailGateway;
    private final TemplateEngine templateEngine;
    private final UserClient userClient;

    @KafkaListener(topics = "sale.inquiry.new", groupId = "notification-inquiry-group")
    public void onInquiryCreated(String message) {
        try {
            JsonNode j = objectMapper.readTree(message);

            String buyerName = j.has("buyerName") ? j.get("buyerName").asText("A buyer") : "A buyer";
            String buyerPhone = j.has("buyerPhone") ? j.get("buyerPhone").asText("") : "";
            String buyerEmail = j.has("buyerEmail") ? j.get("buyerEmail").asText("") : "";
            String inquiryMessage = j.has("message") ? j.get("message").asText("") : "";
            String sellerId = j.has("sellerId") ? j.get("sellerId").asText("") : "";

            // Get seller info
            String sellerEmail = null;
            String sellerName = "Host";
            String sellerPhone = null;
            if (!sellerId.isBlank()) {
                try {
                    UserClient.UserInfo seller = userClient.getUser(sellerId);
                    if (seller != null) {
                        sellerEmail = seller.email();
                        sellerName = seller.name() != null ? seller.name() : "Host";
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch seller info for {}: {}", sellerId, e.getMessage());
                }
            }

            // 1. Send notification to seller (if email available)
            if (sellerEmail != null && !sellerEmail.isBlank()) {
                Context sellerCtx = new Context();
                sellerCtx.setVariable("sellerName", sellerName);
                sellerCtx.setVariable("buyerName", buyerName);
                sellerCtx.setVariable("buyerPhone", buyerPhone);
                sellerCtx.setVariable("buyerEmail", buyerEmail);
                sellerCtx.setVariable("message", inquiryMessage.isBlank() ? "No message provided" : inquiryMessage);

                String html = templateEngine.process("email/inquiry-received", sellerCtx);
                emailGateway.send(sellerEmail, "New Property Inquiry from " + buyerName, html);
                log.info("Inquiry notification sent to seller {} ({})", sellerId, sellerEmail);
            } else {
                log.warn("Seller {} has no email — skipping seller notification", sellerId);
            }

            // 2. Send confirmation to buyer (always, if buyer email provided)
            if (buyerEmail != null && !buyerEmail.isBlank()) {
                Context buyerCtx = new Context();
                buyerCtx.setVariable("buyerName", buyerName);
                buyerCtx.setVariable("sellerName", sellerName);
                buyerCtx.setVariable("message", inquiryMessage.isBlank() ? "No message provided" : inquiryMessage);

                String buyerHtml = templateEngine.process("email/inquiry-confirmation", buyerCtx);
                emailGateway.send(buyerEmail, "Your Inquiry Has Been Sent - Safar", buyerHtml);
                log.info("Inquiry confirmation sent to buyer {}", buyerEmail);
            }

        } catch (Exception e) {
            log.error("Failed to process inquiry notification: {}", e.getMessage(), e);
        }
    }
}
