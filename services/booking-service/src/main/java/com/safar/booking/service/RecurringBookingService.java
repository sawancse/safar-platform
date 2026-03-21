package com.safar.booking.service;

import com.safar.booking.dto.CreateRecurringRequest;
import com.safar.booking.entity.RecurringBooking;
import com.safar.booking.entity.enums.RecurringFrequency;
import com.safar.booking.entity.enums.RecurringStatus;
import com.safar.booking.repository.RecurringBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringBookingService {

    private final RecurringBookingRepository recurringRepo;
    private final KafkaTemplate<String, String> kafka;

    @Transactional
    public RecurringBooking createRecurring(UUID guestId, CreateRecurringRequest req) {
        LocalDate nextDate = computeNextFromStart(req.frequency(), req.startDate(), req.dayOfWeek());

        RecurringBooking recurring = RecurringBooking.builder()
                .guestId(guestId)
                .listingId(req.listingId())
                .frequency(req.frequency())
                .checkInTime(req.checkInTime())
                .checkOutTime(req.checkOutTime())
                .dayOfWeek(req.dayOfWeek())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(RecurringStatus.ACTIVE)
                .noticeDays(7)
                .totalPaidPaise(0L)
                .nextBookingDate(nextDate)
                .build();

        RecurringBooking saved = recurringRepo.save(recurring);
        kafka.send("booking.recurring.created", saved.getId().toString());
        log.info("Recurring booking {} created for guest {} on listing {}",
                saved.getId(), guestId, req.listingId());
        return saved;
    }

    @Transactional
    public RecurringBooking cancel(UUID guestId, UUID recurringId) {
        RecurringBooking recurring = recurringRepo.findByIdAndGuestId(recurringId, guestId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Recurring booking not found: " + recurringId));

        recurring.setStatus(RecurringStatus.CANCELLED);
        RecurringBooking saved = recurringRepo.save(recurring);

        kafka.send("booking.recurring.cancelled", recurringId.toString());
        log.info("Recurring booking {} cancelled by guest {}", recurringId, guestId);
        return saved;
    }

    public List<RecurringBooking> getGuestRecurringBookings(UUID guestId) {
        return recurringRepo.findByGuestId(guestId);
    }

    /**
     * Compute the next booking date based on the current next date and frequency.
     */
    public LocalDate computeNext(RecurringBooking rb) {
        LocalDate current = rb.getNextBookingDate();
        return switch (rb.getFrequency()) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    /**
     * Compute the initial next booking date from the start date.
     */
    private LocalDate computeNextFromStart(RecurringFrequency frequency, LocalDate startDate,
                                            Integer dayOfWeek) {
        return startDate;
    }
}
