package com.safar.chef.scheduler;

import com.safar.chef.entity.ChefBooking;
import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.repository.ChefBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChefBookingScheduler {

    private final ChefBookingRepository bookingRepo;
    private final KafkaTemplate<String, String> kafka;

    // Auto-expire unpaid bookings after 2 hours
    @Scheduled(fixedRate = 300000) // every 5 min
    @Transactional
    public void autoExpireUnpaidBookings() {
        List<ChefBooking> unpaid = bookingRepo.findByStatus(ChefBookingStatus.PENDING_PAYMENT);
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(2);

        for (ChefBooking booking : unpaid) {
            if (booking.getCreatedAt() != null && booking.getCreatedAt().isBefore(cutoff)) {
                booking.setStatus(ChefBookingStatus.CANCELLED);
                booking.setCancellationReason("Auto-expired: payment not received within 2 hours");
                booking.setCancelledAt(OffsetDateTime.now());
                bookingRepo.save(booking);
                log.info("Auto-expired unpaid booking: {} ref={}", booking.getId(), booking.getBookingRef());

                try {
                    kafka.send("chef.booking.cancelled", booking.getId().toString(),
                            buildEventJson(booking, "Auto-expired: payment not received within 2 hours"));
                } catch (Exception e) {
                    log.warn("Kafka send failed for auto-expire: {}", e.getMessage());
                }
            }
        }
    }

    // Send reminder for tomorrow's bookings (runs daily at 6 PM)
    @Scheduled(cron = "0 0 18 * * *")
    @Transactional
    public void sendBookingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<ChefBooking> confirmed = bookingRepo.findByStatus(ChefBookingStatus.CONFIRMED);

        for (ChefBooking booking : confirmed) {
            if (tomorrow.equals(booking.getServiceDate()) && !Boolean.TRUE.equals(booking.getReminderSent())) {
                booking.setReminderSent(true);
                bookingRepo.save(booking);

                try {
                    kafka.send("chef.booking.reminder", booking.getId().toString(),
                            buildEventJson(booking, null));
                } catch (Exception e) {
                    log.warn("Kafka send failed for reminder: {}", e.getMessage());
                }
                log.info("Reminder sent for booking {} on {}", booking.getBookingRef(), tomorrow);
            }
        }
    }

    private String buildEventJson(ChefBooking b, String cancellationReason) {
        return String.format(
                "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"chefId\":\"%s\",\"customerId\":\"%s\","
                + "\"chefName\":\"%s\",\"customerName\":\"%s\",\"serviceDate\":\"%s\",\"serviceTime\":\"%s\","
                + "\"mealType\":\"%s\",\"status\":\"%s\",\"totalAmountPaise\":%d,\"advanceAmountPaise\":%d,"
                + "\"balanceAmountPaise\":%d,\"paymentStatus\":\"%s\",\"city\":\"%s\",\"cancellationReason\":\"%s\"}",
                b.getId(), b.getBookingRef(), b.getChefId(), b.getCustomerId(),
                b.getChefName() != null ? b.getChefName() : "",
                b.getCustomerName() != null ? b.getCustomerName() : "",
                b.getServiceDate(), b.getServiceTime() != null ? b.getServiceTime() : "",
                b.getMealType() != null ? b.getMealType() : "",
                b.getStatus(), b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0,
                b.getAdvanceAmountPaise() != null ? b.getAdvanceAmountPaise() : 0,
                b.getBalanceAmountPaise() != null ? b.getBalanceAmountPaise() : 0,
                b.getPaymentStatus() != null ? b.getPaymentStatus() : "UNPAID",
                b.getCity() != null ? b.getCity() : "",
                cancellationReason != null ? cancellationReason : "");
    }
}
