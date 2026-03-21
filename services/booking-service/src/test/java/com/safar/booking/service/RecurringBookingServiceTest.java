package com.safar.booking.service;

import com.safar.booking.dto.CreateRecurringRequest;
import com.safar.booking.entity.RecurringBooking;
import com.safar.booking.entity.enums.RecurringFrequency;
import com.safar.booking.entity.enums.RecurringStatus;
import com.safar.booking.repository.RecurringBookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringBookingServiceTest {

    @Mock RecurringBookingRepository recurringRepo;
    @Mock KafkaTemplate<String, String> kafka;

    @InjectMocks RecurringBookingService recurringService;

    private final UUID guestId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    /**
     * Test 1: Create recurring booking success — saves and sends Kafka event.
     */
    @Test
    void createRecurring_success_savesAndSendsKafka() {
        CreateRecurringRequest req = new CreateRecurringRequest(
                listingId,
                RecurringFrequency.WEEKLY,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                1, // Monday
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 31)
        );

        UUID savedId = UUID.randomUUID();
        when(recurringRepo.save(any(RecurringBooking.class))).thenAnswer(inv -> {
            RecurringBooking rb = inv.getArgument(0);
            rb.setId(savedId);
            return rb;
        });

        RecurringBooking result = recurringService.createRecurring(guestId, req);

        assertThat(result.getGuestId()).isEqualTo(guestId);
        assertThat(result.getListingId()).isEqualTo(listingId);
        assertThat(result.getFrequency()).isEqualTo(RecurringFrequency.WEEKLY);
        assertThat(result.getStatus()).isEqualTo(RecurringStatus.ACTIVE);
        assertThat(result.getNextBookingDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        verify(kafka).send(eq("booking.recurring.created"), eq(savedId.toString()));
    }

    /**
     * Test 2: Cancel recurring booking — sets CANCELLED and sends Kafka event.
     */
    @Test
    void cancel_existingRecurring_cancelsAndSendsKafka() {
        UUID recurringId = UUID.randomUUID();
        RecurringBooking existing = RecurringBooking.builder()
                .id(recurringId)
                .guestId(guestId)
                .listingId(listingId)
                .frequency(RecurringFrequency.WEEKLY)
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(18, 0))
                .startDate(LocalDate.of(2026, 4, 1))
                .status(RecurringStatus.ACTIVE)
                .noticeDays(7)
                .totalPaidPaise(0L)
                .nextBookingDate(LocalDate.of(2026, 4, 8))
                .build();

        when(recurringRepo.findByIdAndGuestId(recurringId, guestId))
                .thenReturn(Optional.of(existing));
        when(recurringRepo.save(any(RecurringBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        RecurringBooking result = recurringService.cancel(guestId, recurringId);

        assertThat(result.getStatus()).isEqualTo(RecurringStatus.CANCELLED);
        verify(kafka).send(eq("booking.recurring.cancelled"), eq(recurringId.toString()));
    }

    /**
     * Test 3: computeNext — verify correct date calculation for each frequency.
     */
    @Test
    void computeNext_allFrequencies_calculatesCorrectly() {
        LocalDate baseDate = LocalDate.of(2026, 4, 1);

        RecurringBooking daily = RecurringBooking.builder()
                .frequency(RecurringFrequency.DAILY)
                .nextBookingDate(baseDate)
                .build();
        assertThat(recurringService.computeNext(daily)).isEqualTo(LocalDate.of(2026, 4, 2));

        RecurringBooking weekly = RecurringBooking.builder()
                .frequency(RecurringFrequency.WEEKLY)
                .nextBookingDate(baseDate)
                .build();
        assertThat(recurringService.computeNext(weekly)).isEqualTo(LocalDate.of(2026, 4, 8));

        RecurringBooking monthly = RecurringBooking.builder()
                .frequency(RecurringFrequency.MONTHLY)
                .nextBookingDate(baseDate)
                .build();
        assertThat(recurringService.computeNext(monthly)).isEqualTo(LocalDate.of(2026, 5, 1));
    }
}
