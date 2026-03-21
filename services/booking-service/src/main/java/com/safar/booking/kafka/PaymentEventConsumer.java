package com.safar.booking.kafka;

import com.safar.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final BookingService bookingService;

    @KafkaListener(topics = "payment.captured", groupId = "booking-service")
    public void onPaymentCaptured(String bookingId) {
        try {
            bookingService.confirmBooking(UUID.fromString(bookingId));
            log.info("Booking {} confirmed via payment.captured event", bookingId);
        } catch (Exception e) {
            log.error("Failed to confirm booking {}: {}", bookingId, e.getMessage());
        }
    }
}
