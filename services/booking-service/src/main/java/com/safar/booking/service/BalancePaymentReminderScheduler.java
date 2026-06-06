package com.safar.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.entity.Booking;
import com.safar.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Emits balance-payment reminder Kafka events for PARTIAL_PREPAID bookings whose
 * online balance is still owed.
 *
 * Two tiers: T-7 days before check-in, then T-1 day if still unpaid.
 * Dedup is per-tier via {@code balance_reminder_t{7,1}_sent_at} columns set on send,
 * so re-runs and multi-instance schedulers don't double-fire.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalancePaymentReminderScheduler {

    private final BookingRepository bookingRepo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    // Daily at 09:00 IST (server runs in Asia/Kolkata). One firing per day per tier.
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sweep() {
        sweepTier("T7", 7);
        sweepTier("T1", 1);
    }

    private void sweepTier(String tier, int daysAhead) {
        LocalDate target = LocalDate.now().plusDays(daysAhead);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        List<Booking> candidates = bookingRepo.findBalanceReminderCandidates(from, to, tier);
        if (candidates.isEmpty()) return;

        for (Booking b : candidates) {
            try {
                String payload = objectMapper.writeValueAsString(Map.of(
                        "bookingId", b.getId().toString(),
                        "bookingRef", b.getBookingRef() == null ? "" : b.getBookingRef(),
                        "tier", tier,
                        "dueAtPropertyPaise", b.getDueAtPropertyPaise(),
                        "totalAmountPaise", b.getTotalAmountPaise(),
                        "checkIn", b.getCheckIn().toLocalDate().toString(),
                        "guestEmail", b.getGuestEmail() == null ? "" : b.getGuestEmail(),
                        "guestName", buildGuestName(b),
                        "listingTitle", b.getListingTitle() == null ? "" : b.getListingTitle()
                ));
                kafka.send("booking.balance.reminder", b.getId().toString(), payload);

                OffsetDateTime now = OffsetDateTime.now();
                if ("T7".equals(tier)) b.setBalanceReminderT7SentAt(now);
                else b.setBalanceReminderT1SentAt(now);
                bookingRepo.save(b);

                log.info("Balance reminder {} sent for booking {} (due {} paise)",
                        tier, b.getBookingRef(), b.getDueAtPropertyPaise());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize balance reminder for {}: {}", b.getBookingRef(), e.getMessage());
            } catch (Exception e) {
                log.warn("Failed to publish balance reminder for {}: {}", b.getBookingRef(), e.getMessage());
            }
        }
    }

    private String buildGuestName(Booking b) {
        String first = b.getGuestFirstName() == null ? "" : b.getGuestFirstName();
        String last = b.getGuestLastName() == null ? "" : b.getGuestLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Guest" : full;
    }
}
