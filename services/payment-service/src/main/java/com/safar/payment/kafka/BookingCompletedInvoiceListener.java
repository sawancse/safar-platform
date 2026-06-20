package com.safar.payment.kafka;

import com.safar.payment.service.GstInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Generates the host GST invoice when a booking completes.
 *
 * <p>booking-service publishes {@code booking.invoice.requested} from
 * {@code markBookingCompleted} carrying the hostId + booking total. Without this
 * listener the {@code payments.gst_invoices} table stays empty, which makes the
 * host dashboard's Earnings, GST Invoices and TDS tabs all read zero.
 *
 * <p>Generation is idempotent (one invoice per booking) so Kafka redelivery and
 * the one-time backfill never produce duplicates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletedInvoiceListener {

    private final GstInvoiceService gstInvoiceService;

    @KafkaListener(topics = "booking.invoice.requested", groupId = "payment-service")
    public void onBookingCompleted(Map<String, Object> event) {
        try {
            UUID bookingId = UUID.fromString((String) event.get("bookingId"));
            Object hostIdRaw = event.get("hostId");
            Object totalRaw = event.get("totalAmountPaise");
            if (hostIdRaw == null || totalRaw == null) {
                log.warn("Skipping GST invoice for booking {} — missing hostId/total in event", bookingId);
                return;
            }
            UUID hostId = UUID.fromString((String) hostIdRaw);
            long totalAmountPaise = ((Number) totalRaw).longValue();
            if (totalAmountPaise <= 0) {
                log.warn("Skipping GST invoice for booking {} — non-positive total {}", bookingId, totalAmountPaise);
                return;
            }

            // isInterState only affects the CGST/SGST vs IGST split, not the taxable
            // amount the dashboard sums. Booking state pair isn't on the event, so
            // default to intra-state; a tax-profile-aware split can refine this later.
            boolean isInterState = false;

            gstInvoiceService.generateInvoice(hostId, bookingId, totalAmountPaise, isInterState);
            log.info("Ensured GST invoice for completed booking {} (host {}, total {} paise)",
                    bookingId, hostId, totalAmountPaise);
        } catch (Exception e) {
            log.error("Failed to generate GST invoice for booking.invoice.requested event {}: {}",
                    event, e.getMessage());
        }
    }
}
