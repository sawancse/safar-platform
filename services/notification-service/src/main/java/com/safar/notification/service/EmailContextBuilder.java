package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds an EmailContext from BookingInfo + UserInfo data for use with the HTML template system.
 */
@Component
public class EmailContextBuilder {

    private final ToneService toneService;

    // Production: set NOTIFICATION_BASE_URL=https://ysafar.com
    @Value("${notification.base-url}")
    private String baseUrl;

    public EmailContextBuilder(ToneService toneService) {
        this.toneService = toneService;
    }

    /**
     * Build a fully populated EmailContext from booking and user data.
     *
     * @param booking             booking info from BookingClient
     * @param guest               guest info from UserClient (nullable — falls back to booking fields)
     * @param host                host info from UserClient (nullable)
     * @param listingType         listing type string (e.g. HOME, VILLA, PG) — nullable
     * @param discoveryCategories comma-separated discovery categories — nullable
     * @return populated EmailContext with tone determined automatically
     */
    public EmailContext buildBookingContext(BookingClient.BookingInfo booking,
                                            UserClient.UserInfo guest,
                                            UserClient.UserInfo host,
                                            String listingType,
                                            String discoveryCategories) {
        EmailContext ctx = new EmailContext();

        // Guest info
        ctx.setGuestName(booking.guestName());
        String guestEmail = booking.guestEmail();
        if ((guestEmail == null || guestEmail.isBlank()) && guest != null) {
            guestEmail = guest.email();
        }
        ctx.setGuestEmail(guestEmail);

        // Host info
        if (host != null) {
            ctx.setHostName(host.name() != null ? host.name() : "Your Host");
            ctx.setHostEmail(host.email());
            ctx.setHostPhone(host.phone());
        }

        // Booking info
        ctx.setBookingRef(booking.bookingRef());
        if (booking.checkIn() != null && !booking.checkIn().isBlank()) {
            ctx.setCheckIn(formatDate(booking.checkIn()));
        }
        if (booking.checkOut() != null && !booking.checkOut().isBlank()) {
            ctx.setCheckOut(formatDate(booking.checkOut()));
        }
        ctx.setNights(booking.nights());
        ctx.setGuests(booking.guestsCount());
        ctx.setAdults(booking.adultsCount());
        ctx.setChildren(booking.childrenCount());
        ctx.setInfants(booking.infantsCount());
        ctx.setRooms(booking.roomsCount());
        ctx.setPaymentMode(booking.paymentMode());
        if (booking.cancellationReason() != null && !booking.cancellationReason().isBlank()) {
            ctx.setCancellationReason(booking.cancellationReason());
        }
        ctx.setPricingUnit(booking.pricingUnit());
        ctx.setTotalAmount(formatPaiseToRupeesWithSymbol(booking.totalAmountPaise()));
        if (booking.baseAmountPaise() > 0)
            ctx.setBaseAmount(formatPaiseToRupeesWithSymbol(booking.baseAmountPaise()));
        if (booking.gstAmountPaise() > 0)
            ctx.setGstAmount(formatPaiseToRupeesWithSymbol(booking.gstAmountPaise()));
        if (booking.cleaningFeePaise() > 0)
            ctx.setCleaningFee(formatPaiseToRupeesWithSymbol(booking.cleaningFeePaise()));
        if (booking.platformFeePaise() > 0)
            ctx.setPlatformFee(formatPaiseToRupeesWithSymbol(booking.platformFeePaise()));
        if (booking.insuranceAmountPaise() > 0)
            ctx.setInsuranceAmount(formatPaiseToRupeesWithSymbol(booking.insuranceAmountPaise()));
        if (booking.securityDepositPaise() > 0)
            ctx.setSecurityDeposit(formatPaiseToRupeesWithSymbol(booking.securityDepositPaise()));
        if (booking.inclusionsTotalPaise() > 0)
            ctx.setInclusionsTotal(formatPaiseToRupeesWithSymbol(booking.inclusionsTotalPaise()));

        // Listing info
        if (booking.listingTitle() != null && !booking.listingTitle().isBlank()) {
            ctx.setListingTitle(booking.listingTitle());
        }
        if (booking.listingCity() != null && !booking.listingCity().isBlank()) {
            ctx.setListingCity(booking.listingCity());
        }
        if (booking.listingAddress() != null && !booking.listingAddress().isBlank()) {
            ctx.setListingAddress(booking.listingAddress());
        }
        if (listingType != null) {
            ctx.setListingType(listingType);
        }
        if (discoveryCategories != null) {
            ctx.setDiscoveryCategories(discoveryCategories);
        }

        // Determine tone
        UUID guestId = parseUuidSafe(booking.guestId());
        boolean isMedical = "MEDICAL".equalsIgnoreCase(listingType);
        String tone = toneService.determineTone(
                guestId != null ? guestId : UUID.randomUUID(),
                listingType,
                discoveryCategories,
                isMedical
        );
        ctx.setTone(tone);

        // Standard URLs — baseUrl from notification.base-url config
        ctx.setBookingUrl(baseUrl + "/dashboard/bookings/" + booking.bookingRef());
        ctx.setDashboardUrl(baseUrl + "/dashboard");
        ctx.setReviewUrl(baseUrl + "/dashboard/bookings/" + booking.bookingRef() + "/review");
        ctx.setFeedbackUrl(baseUrl + "/feedback");
        ctx.setUnsubscribeUrl(baseUrl + "/settings/email-preferences");
        ctx.setPreferencesUrl(baseUrl + "/settings/email-preferences");

        return ctx;
    }

    /**
     * Format an ISO-8601 datetime string to "Mon 15 Jun 2026" for display.
     * Falls back to the raw string if parsing fails.
     */
    private static String formatDate(String iso) {
        if (iso == null || iso.isBlank()) return iso;
        try {
            String trimmed = iso.length() > 10 ? iso.substring(0, 10) : iso;
            java.time.LocalDate d = java.time.LocalDate.parse(trimmed);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    /**
     * Format paise amount to rupee display string (e.g. 150000 → "1,500.00").
     */
    public static String formatPaiseToRupees(long paise) {
        double rupees = paise / 100.0;
        return String.format("%,.2f", rupees);
    }

    /**
     * Format paise amount to rupee display string with symbol (e.g. 150000 → "₹1,500.00").
     */
    public static String formatPaiseToRupeesWithSymbol(long paise) {
        return "₹" + formatPaiseToRupees(paise);
    }

    private static UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
