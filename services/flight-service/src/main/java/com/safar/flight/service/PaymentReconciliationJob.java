package com.safar.flight.service;

import com.safar.flight.entity.FlightBooking;
import com.safar.flight.entity.FlightBookingStatus;
import com.safar.flight.repository.FlightBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Daily reconciliation: compare Razorpay-collected vs provider-settled
 * amounts per booking. Logs drift; alerts if drift exceeds threshold.
 *
 * Provider-agnostic — works the same for TBO, TripJack, TravClan when
 * each goes live. Per-provider settlement-side query is provider-specific
 * (TODO blocks below); the comparison + alerting framework is shared.
 *
 * Daily at 03:30 IST (after settlement windows for most Indian providers
 * which run end-of-day). Tunable via env if individual partners settle
 * on different schedules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    /** Drift threshold in paise above which we WARN-log (escalate to ops alert later). */
    private static final long DRIFT_ALERT_THRESHOLD_PAISE = 50_00_000L; // ₹50,000

    private final FlightBookingRepository bookingRepository;

    @Scheduled(cron = "${flight.recon.cron:0 30 3 * * *}", zone = "Asia/Kolkata")
    public void run() {
        log.info("PaymentReconciliationJob tick (daily 03:30 IST)");

        // Look at bookings from yesterday (settlement typically T+1)
        Instant since = Instant.now().minus(2, ChronoUnit.DAYS);
        List<FlightBooking> recent = bookingRepository.findByStatus(FlightBookingStatus.CONFIRMED).stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(since))
                .toList();

        if (recent.isEmpty()) {
            log.info("PaymentReconciliationJob: no recent confirmed bookings to reconcile");
            return;
        }

        long totalDriftPaise = 0;
        int reconciled = 0;
        int driftRows = 0;
        for (FlightBooking booking : recent) {
            try {
                long razorpayCollected = booking.getTotalAmountPaise() != null
                        ? booking.getTotalAmountPaise() : 0L;
                long providerSettled = fetchProviderSettlement(booking);

                long drift = Math.abs(razorpayCollected - providerSettled);
                if (drift > 0) {
                    driftRows++;
                    totalDriftPaise += drift;
                    log.warn("Settlement drift on booking {}: razorpay={} provider={} drift={} paise",
                            booking.getBookingRef(), razorpayCollected, providerSettled, drift);
                }
                reconciled++;
            } catch (Exception e) {
                log.warn("Recon failed for booking {}: {}", booking.getBookingRef(), e.getMessage());
            }
        }

        log.info("PaymentReconciliationJob complete: {} reconciled, {} drift rows, total drift {} paise",
                reconciled, driftRows, totalDriftPaise);

        if (totalDriftPaise > DRIFT_ALERT_THRESHOLD_PAISE) {
            // TODO: integrate with Slack/PagerDuty alerting once those channels exist.
            log.error("DRIFT ALERT — total recon drift {} paise exceeds threshold {} paise",
                    totalDriftPaise, DRIFT_ALERT_THRESHOLD_PAISE);
        }
    }

    /**
     * Fetch the settled amount from the provider that actually issued the
     * booking. Per-provider settlement query is provider-specific.
     *
     * Currently:
     *  - AMADEUS  : free GDS — no settlement endpoint. Returns Razorpay amount as proxy.
     *  - DUFFEL   : has Settlements API but not in active use. TODO when reactivated.
     *  - TBO      : has /Settlement endpoint (per dealint.tboair.com docs). TODO once creds land.
     *  - TRIPJACK / TRAVCLAN : same — TODO once creds land.
     */
    private long fetchProviderSettlement(FlightBooking booking) {
        if (booking.getProvider() == null) return booking.getTotalAmountPaise();
        switch (booking.getProvider()) {
            case "TBO":
                // TODO(tbo-creds): wire TBO /Settlement endpoint
                //   POST /Settlement/GetSettledAmount
                //   Body: { TokenId, BookingId: externalOrderId }
                //   Response: { SettledAmount, SettlementDate, Status }
                //   For now, treat as drift-free (returns Razorpay amount).
                return booking.getTotalAmountPaise();
            case "DUFFEL":
                // Out of active use; if reactivated, wire GET /air/orders/{id}/payments
                return booking.getTotalAmountPaise();
            case "AMADEUS":
            default:
                return booking.getTotalAmountPaise();
        }
    }
}
