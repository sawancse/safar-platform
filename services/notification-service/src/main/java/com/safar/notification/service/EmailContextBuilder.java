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
        }

        // Booking info
        ctx.setBookingRef(booking.bookingRef());

        // Listing info
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
