package com.safar.booking.service;

import com.safar.booking.entity.LiveAnywhereStay;
import com.safar.booking.entity.LiveAnywhereSubscription;
import com.safar.booking.entity.enums.SubscriptionStatus;
import com.safar.booking.repository.LiveAnywhereStayRepository;
import com.safar.booking.repository.LiveAnywhereSubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveAnywhereService {

    static final long SUBSCRIPTION_PRICE_PAISE = 499_900L;
    static final long MAX_COVERED_PAISE = 300_000L;
    static final int MAX_NIGHTS_MONTH = 30;

    private final LiveAnywhereSubRepository subRepository;
    private final LiveAnywhereStayRepository stayRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public LiveAnywhereSubscription subscribe(UUID guestId) {
        if (subRepository.existsByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("Guest already has an active Live Anywhere subscription");
        }

        LiveAnywhereSubscription subscription = LiveAnywhereSubscription.builder()
                .guestId(guestId)
                .status(SubscriptionStatus.ACTIVE)
                .nightsUsedThisMonth(0)
                .maxNightsPerMonth(MAX_NIGHTS_MONTH)
                .maxCoveredPaise(MAX_COVERED_PAISE)
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .build();

        LiveAnywhereSubscription saved = subRepository.save(subscription);

        String payload = String.format(
                "{\"subscriptionId\":\"%s\",\"guestId\":\"%s\",\"pricePaise\":%d}",
                saved.getId(), guestId, SUBSCRIPTION_PRICE_PAISE);
        kafkaTemplate.send("live_anywhere.subscribed", saved.getId().toString(), payload);
        log.info("Live Anywhere subscription {} created for guest {}", saved.getId(), guestId);

        return saved;
    }

    @Transactional
    public LiveAnywhereSubscription cancel(UUID guestId) {
        LiveAnywhereSubscription subscription = subRepository
                .findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active subscription found for guest: " + guestId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(OffsetDateTime.now());

        LiveAnywhereSubscription saved = subRepository.save(subscription);
        log.info("Live Anywhere subscription {} cancelled for guest {}", saved.getId(), guestId);
        return saved;
    }

    @Transactional
    public LiveAnywhereStay bookWithSubscription(UUID guestId, UUID listingId,
                                                  long listingPricePerNightPaise, int nights) {
        LiveAnywhereSubscription subscription = subRepository
                .findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active subscription found for guest: " + guestId));

        int remaining = subscription.getMaxNightsPerMonth() - subscription.getNightsUsedThisMonth();
        if (nights > remaining) {
            throw new IllegalStateException(
                    String.format("Monthly cap exceeded: %d nights remaining, %d requested", remaining, nights));
        }

        long coveredPerNight = Math.min(listingPricePerNightPaise, subscription.getMaxCoveredPaise());
        long topupPerNight = listingPricePerNightPaise - coveredPerNight;
        long totalCovered = coveredPerNight * nights;
        long totalTopup = topupPerNight * nights;

        LiveAnywhereStay stay = LiveAnywhereStay.builder()
                .subscriptionId(subscription.getId())
                .bookingId(listingId) // using listingId as booking reference
                .checkIn(LocalDate.now())
                .checkOut(LocalDate.now().plusDays(nights))
                .nights(nights)
                .listingPrice(listingPricePerNightPaise)
                .coveredPaise(totalCovered)
                .guestTopup(totalTopup)
                .build();

        LiveAnywhereStay saved = stayRepository.save(stay);

        subscription.setNightsUsedThisMonth(subscription.getNightsUsedThisMonth() + nights);
        subscription.setCurrentStayId(saved.getId());
        subRepository.save(subscription);

        log.info("Live Anywhere stay {} booked: {} nights, covered={}, topup={}",
                saved.getId(), nights, totalCovered, totalTopup);
        return saved;
    }

    public LiveAnywhereSubscription getSubscription(UUID guestId) {
        return subRepository.findByGuestIdAndStatus(guestId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active subscription found for guest: " + guestId));
    }

    public List<LiveAnywhereStay> getStays(UUID guestId) {
        LiveAnywhereSubscription subscription = getSubscription(guestId);
        return stayRepository.findBySubscriptionId(subscription.getId());
    }
}
