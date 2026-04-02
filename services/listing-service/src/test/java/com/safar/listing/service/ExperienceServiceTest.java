package com.safar.listing.service;

import com.safar.listing.client.UserNameClient;
import com.safar.listing.dto.ExperienceBookingRequest;
import com.safar.listing.entity.Experience;
import com.safar.listing.entity.ExperienceBooking;
import com.safar.listing.entity.ExperienceSession;
import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;
import com.safar.listing.entity.enums.SessionStatus;
import com.safar.listing.repository.ExperienceBookingRepository;
import com.safar.listing.repository.ExperienceRepository;
import com.safar.listing.repository.ExperienceSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock ExperienceRepository experienceRepository;
    @Mock ExperienceSessionRepository sessionRepository;
    @Mock ExperienceBookingRepository bookingRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock UserNameClient userNameClient;

    @InjectMocks ExperienceService experienceService;

    private final UUID guestId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    /**
     * Test 1: Spot deduction correct — bookedSpots incremented by numGuests.
     */
    @Test
    void bookExperience_deductsSpots_correctly() {
        ExperienceSession session = ExperienceSession.builder()
                .id(sessionId)
                .experienceId(experienceId)
                .sessionDate(LocalDate.of(2026, 5, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .availableSpots(10)
                .bookedSpots(3)
                .status(SessionStatus.OPEN)
                .build();

        Experience experience = Experience.builder()
                .id(experienceId)
                .hostId(hostId)
                .title("Cooking Class")
                .description("Learn to cook")
                .category(ExperienceCategory.CULINARY)
                .city("Mumbai")
                .durationHours(BigDecimal.valueOf(3.0))
                .pricePaise(150_000L)
                .status(ExperienceStatus.ACTIVE)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(experienceRepository.findById(experienceId)).thenReturn(Optional.of(experience));
        when(bookingRepository.save(any(ExperienceBooking.class))).thenAnswer(inv -> {
            ExperienceBooking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(sessionRepository.save(any(ExperienceSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienceBookingRequest req = new ExperienceBookingRequest(sessionId, 4, null);
        experienceService.bookExperience(guestId, req);

        // bookedSpots should go from 3 to 7
        assertThat(session.getBookedSpots()).isEqualTo(7);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.OPEN);
    }

    /**
     * Test 2: Session marked FULL when capacity reached.
     */
    @Test
    void bookExperience_capacityReached_marksSessionFull() {
        ExperienceSession session = ExperienceSession.builder()
                .id(sessionId)
                .experienceId(experienceId)
                .sessionDate(LocalDate.of(2026, 5, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .availableSpots(5)
                .bookedSpots(3)
                .status(SessionStatus.OPEN)
                .build();

        Experience experience = Experience.builder()
                .id(experienceId)
                .hostId(hostId)
                .title("Yoga Retreat")
                .description("Morning yoga")
                .category(ExperienceCategory.WELLNESS)
                .city("Goa")
                .durationHours(BigDecimal.valueOf(2.0))
                .pricePaise(100_000L)
                .status(ExperienceStatus.ACTIVE)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(experienceRepository.findById(experienceId)).thenReturn(Optional.of(experience));
        when(bookingRepository.save(any(ExperienceBooking.class))).thenAnswer(inv -> {
            ExperienceBooking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(sessionRepository.save(any(ExperienceSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienceBookingRequest req = new ExperienceBookingRequest(sessionId, 2, null);
        experienceService.bookExperience(guestId, req);

        // 3 + 2 = 5 = available, so FULL
        assertThat(session.getBookedSpots()).isEqualTo(5);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.FULL);
    }

    /**
     * Test 3: 20% platform fee calculation.
     */
    @Test
    void bookExperience_calculatesFeesCorrectly() {
        ExperienceSession session = ExperienceSession.builder()
                .id(sessionId)
                .experienceId(experienceId)
                .sessionDate(LocalDate.of(2026, 5, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .availableSpots(10)
                .bookedSpots(0)
                .status(SessionStatus.OPEN)
                .build();

        Experience experience = Experience.builder()
                .id(experienceId)
                .hostId(hostId)
                .title("Heritage Walk")
                .description("Old city walk")
                .category(ExperienceCategory.CULTURAL)
                .city("Delhi")
                .durationHours(BigDecimal.valueOf(2.5))
                .pricePaise(200_000L)
                .status(ExperienceStatus.ACTIVE)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(experienceRepository.findById(experienceId)).thenReturn(Optional.of(experience));
        when(bookingRepository.save(any(ExperienceBooking.class))).thenAnswer(inv -> {
            ExperienceBooking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(sessionRepository.save(any(ExperienceSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ExperienceBookingRequest req = new ExperienceBookingRequest(sessionId, 3, null);
        ExperienceBooking result = experienceService.bookExperience(guestId, req);

        // total = 200_000 * 3 = 600_000
        assertThat(result.getTotalPaise()).isEqualTo(600_000L);
        // platform fee = 600_000 * 20 / 100 = 120_000
        assertThat(result.getPlatformFeePaise()).isEqualTo(120_000L);
        // host payout = 600_000 - 120_000 = 480_000
        assertThat(result.getHostPayoutPaise()).isEqualTo(480_000L);
        // ref starts with SAF-EXP-
        assertThat(result.getRef()).startsWith("SAF-EXP-");
        assertThat(result.getRef()).hasSize(14); // "SAF-EXP-" (8) + 6 chars
    }
}
