package com.safar.booking.service;

import com.safar.booking.entity.LiveAnywhereStay;
import com.safar.booking.entity.LiveAnywhereSubscription;
import com.safar.booking.entity.enums.SubscriptionStatus;
import com.safar.booking.repository.LiveAnywhereStayRepository;
import com.safar.booking.repository.LiveAnywhereSubRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveAnywhereServiceTest {

    @Mock LiveAnywhereSubRepository subRepository;
    @Mock LiveAnywhereStayRepository stayRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks LiveAnywhereService liveAnywhereService;

    private final UUID guestId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    /**
     * Test 1: Monthly cap enforced — booking more nights than remaining throws exception.
     */
    @Test
    void bookWithSubscription_exceedsMonthlyNightsCap_throwsException() {
        LiveAnywhereSubscription subscription = LiveAnywhereSubscription.builder()
                .id(UUID.randomUUID())
                .guestId(guestId)
                .status(SubscriptionStatus.ACTIVE)
                .nightsUsedThisMonth(28)
                .maxNightsPerMonth(30)
                .maxCoveredPaise(300_000L)
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .build();

        when(subRepository.findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        assertThatThrownBy(() ->
                liveAnywhereService.bookWithSubscription(guestId, listingId, 250_000L, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Monthly cap exceeded");
    }

    /**
     * Test 2: Topup calculated correctly when listing price exceeds covered amount.
     */
    @Test
    void bookWithSubscription_listingExceedsCovered_calculatesTopup() {
        LiveAnywhereSubscription subscription = LiveAnywhereSubscription.builder()
                .id(UUID.randomUUID())
                .guestId(guestId)
                .status(SubscriptionStatus.ACTIVE)
                .nightsUsedThisMonth(0)
                .maxNightsPerMonth(30)
                .maxCoveredPaise(300_000L)
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .build();

        when(subRepository.findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(stayRepository.save(any(LiveAnywhereStay.class)))
                .thenAnswer(inv -> {
                    LiveAnywhereStay stay = inv.getArgument(0);
                    stay.setId(UUID.randomUUID());
                    return stay;
                });
        when(subRepository.save(any(LiveAnywhereSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Listing price 500_000 paise/night, covered max 300_000, so topup = 200_000 per night
        LiveAnywhereStay result = liveAnywhereService.bookWithSubscription(
                guestId, listingId, 500_000L, 3);

        // covered = 300_000 * 3 = 900_000
        assertThat(result.getCoveredPaise()).isEqualTo(900_000L);
        // topup = (500_000 - 300_000) * 3 = 600_000
        assertThat(result.getGuestTopup()).isEqualTo(600_000L);
        assertThat(result.getNights()).isEqualTo(3);
    }

    /**
     * Test 3: Zero topup when listing price is within covered amount.
     */
    @Test
    void bookWithSubscription_listingWithinCovered_zeroTopup() {
        LiveAnywhereSubscription subscription = LiveAnywhereSubscription.builder()
                .id(UUID.randomUUID())
                .guestId(guestId)
                .status(SubscriptionStatus.ACTIVE)
                .nightsUsedThisMonth(0)
                .maxNightsPerMonth(30)
                .maxCoveredPaise(300_000L)
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .build();

        when(subRepository.findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(stayRepository.save(any(LiveAnywhereStay.class)))
                .thenAnswer(inv -> {
                    LiveAnywhereStay stay = inv.getArgument(0);
                    stay.setId(UUID.randomUUID());
                    return stay;
                });
        when(subRepository.save(any(LiveAnywhereSubscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Listing price 200_000 paise/night, which is <= covered 300_000
        LiveAnywhereStay result = liveAnywhereService.bookWithSubscription(
                guestId, listingId, 200_000L, 2);

        // covered = 200_000 * 2 = 400_000 (min of listing price and max covered)
        assertThat(result.getCoveredPaise()).isEqualTo(400_000L);
        // topup = 0 since listing <= covered
        assertThat(result.getGuestTopup()).isEqualTo(0L);
        assertThat(result.getNights()).isEqualTo(2);
    }
}
