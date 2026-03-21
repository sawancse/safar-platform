package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MedicalBookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "medical.booking.created", groupId = "notification-service")
    public void handleMedicalBookingCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String bookingId = event.get("bookingId").asText();
            String guestId = event.get("guestId").asText();
            String procedureName = event.has("procedureName") ? event.get("procedureName").asText() : "Medical Procedure";
            String hospitalName = event.has("hospitalName") ? event.get("hospitalName").asText() : "";
            String procedureDate = event.has("procedureDate") ? event.get("procedureDate").asText() : "";

            notificationService.sendMedicalBookingConfirmation(guestId, bookingId, procedureName, hospitalName, procedureDate);
            log.info("Medical booking notification sent for booking {}", bookingId);
        } catch (Exception e) {
            log.error("Failed to process medical booking event: {}", e.getMessage(), e);
        }
    }
}
