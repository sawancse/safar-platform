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
                            String.format("{\"bookingId\":\"%s\",\"reason\":\"auto_expired\"}", booking.getId()));
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
                            String.format("{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"chefName\":\"%s\","
                                    + "\"customerName\":\"%s\",\"serviceDate\":\"%s\",\"serviceTime\":\"%s\"}",
                                    booking.getId(), booking.getBookingRef(),
                                    booking.getChefName() != null ? booking.getChefName() : "",
                                    booking.getCustomerName() != null ? booking.getCustomerName() : "",
                                    booking.getServiceDate(), booking.getServiceTime()));
                } catch (Exception e) {
                    log.warn("Kafka send failed for reminder: {}", e.getMessage());
                }
                log.info("Reminder sent for booking {} on {}", booking.getBookingRef(), tomorrow);
            }
        }
    }
}
