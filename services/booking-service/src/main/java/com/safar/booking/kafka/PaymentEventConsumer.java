package com.safar.booking.kafka;

import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final BookingService bookingService;
    private final BookingRepository bookingRepo;

    @KafkaListener(topics = "payment.captured", groupId = "booking-service")
    public void onPaymentCaptured(String bookingId) {
        try {
            UUID id = UUID.fromString(bookingId);
            Optional<Booking> existing = bookingRepo.findById(id);
            // PARTIAL_PREPAID booking that is already CONFIRMED and still has a balance owed →
            // this is the online balance payment, not the initial prepaid capture.
            if (existing.isPresent()) {
                Booking b = existing.get();
                if (b.getStatus() == BookingStatus.CONFIRMED
                        && "PARTIAL_PREPAID".equals(b.getPaymentMode())
                        && b.getDueAtPropertyPaise() != null
                        && b.getDueAtPropertyPaise() > 0) {
                    bookingService.markBalancePaid(id);
                    log.info("Booking {} balance marked paid via payment.captured event", bookingId);
                    return;
                }
            }
            bookingService.confirmBooking(id);
            log.info("Booking {} confirmed via payment.captured event", bookingId);
        } catch (Exception e) {
            log.error("Failed to process payment.captured for booking {}: {}", bookingId, e.getMessage());
        }
    }
}
