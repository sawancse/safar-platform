package com.safar.services.scheduler;

import com.safar.services.entity.EventBooking;
import com.safar.services.repository.EventBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * One-shot startup pass: any event booking past the CONFIRMED gate without a
 * start-job OTP gets one minted. This repairs legacy rows that predate the
 * on-creation OTP feature so the customer's OTP tab can render the share UI
 * instead of falling through to the "not yet" fallback. Going forward, both
 * the creation path (bespoke) and `markAdvancePaid` mint the OTP themselves,
 * so this loop should find zero rows on subsequent boots.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartJobOtpBackfill {

    private final EventBookingRepository eventRepo;
    private static final SecureRandom RANDOM = new SecureRandom();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        List<EventBooking> stale = eventRepo.findMissingStartJobOtp();
        if (stale.isEmpty()) return;
        for (EventBooking eb : stale) {
            eb.setStartJobOtp(String.format("%04d", RANDOM.nextInt(10000)));
        }
        eventRepo.saveAll(stale);
        log.info("Backfilled start-job OTP on {} event bookings missing one", stale.size());
    }
}
