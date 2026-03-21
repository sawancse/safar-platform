package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import com.safar.notification.entity.ScheduledEmail;
import com.safar.notification.repository.ScheduledEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EmailSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(EmailSchedulerService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Production: set NOTIFICATION_BASE_URL=https://safar.com
    @Value("${notification.base-url}")
    private String baseUrl;

    private final ScheduledEmailRepository scheduledEmailRepo;
    private final JourneyChapterService journeyChapterService;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final ToneService toneService;

    public EmailSchedulerService(ScheduledEmailRepository scheduledEmailRepo,
                                  JourneyChapterService journeyChapterService,
                                  BookingClient bookingClient,
                                  UserClient userClient,
                                  ToneService toneService) {
        this.scheduledEmailRepo = scheduledEmailRepo;
        this.journeyChapterService = journeyChapterService;
        this.bookingClient = bookingClient;
        this.userClient = userClient;
        this.toneService = toneService;
    }

    /**
     * Schedule all journey chapter emails when a booking is confirmed.
     */
    public void scheduleBookingJourneyEmails(UUID bookingId, UUID guestId, LocalDateTime checkIn, LocalDateTime checkOut) {
        OffsetDateTime checkInOdt = checkIn.atZone(IST).toOffsetDateTime();
        OffsetDateTime checkOutOdt = checkOut.atZone(IST).toOffsetDateTime();
        OffsetDateTime now = OffsetDateTime.now();

        // Chapter 3: 7 days before check-in (9am IST)
        OffsetDateTime sevenDaysBefore = checkInOdt.minusDays(7).withHour(9).withMinute(0);
        if (sevenDaysBefore.isAfter(now)) {
            scheduleEmail(bookingId, guestId, "CHAPTER_3", sevenDaysBefore, null);
        }

        // Chapter 4: 48 hours before check-in (10am IST)
        OffsetDateTime fortyEightHrsBefore = checkInOdt.minusDays(2).withHour(10).withMinute(0);
        if (fortyEightHrsBefore.isAfter(now)) {
            scheduleEmail(bookingId, guestId, "CHAPTER_4", fortyEightHrsBefore, null);
        }

        // Chapter 5: Check-in day (8am IST)
        OffsetDateTime checkInDay = checkInOdt.withHour(8).withMinute(0);
        if (checkInDay.isAfter(now)) {
            scheduleEmail(bookingId, guestId, "CHAPTER_5", checkInDay, null);
        }

        // Chapter 6: Mid-stay check (only if stay is 2+ nights)
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        if (nights >= 2) {
            long midPoint = Math.max(1, nights / 2);
            OffsetDateTime midStay = checkInOdt.plusDays(midPoint).withHour(11).withMinute(0);
            if (midStay.isAfter(now) && midStay.isBefore(checkOutOdt)) {
                scheduleEmail(bookingId, guestId, "CHAPTER_6", midStay, null);
            }
        }

        // Chapter 7: Check-out day (8am IST)
        OffsetDateTime checkOutDay = checkOutOdt.withHour(8).withMinute(0);
        if (checkOutDay.isAfter(now)) {
            scheduleEmail(bookingId, guestId, "CHAPTER_7", checkOutDay, null);
        }

        // Chapter 8: Post-stay review prompt (24 hours after checkout)
        OffsetDateTime postStay = checkOutOdt.plusDays(1).withHour(10).withMinute(0);
        scheduleEmail(bookingId, guestId, "CHAPTER_8", postStay, null);

        // Chapter 9: Re-engagement (30 days after checkout)
        OffsetDateTime reEngagement = checkOutOdt.plusDays(30).withHour(10).withMinute(0);
        scheduleEmail(bookingId, guestId, "CHAPTER_9", reEngagement, null);

        log.info("Scheduled journey emails for booking {} (check-in: {}, check-out: {})", bookingId, checkIn, checkOut);
    }

    /**
     * Cancel all pending scheduled emails for a cancelled booking.
     */
    @Transactional
    public void cancelBookingEmails(UUID bookingId) {
        int cancelled = scheduledEmailRepo.cancelByBookingId(bookingId);
        log.info("Cancelled {} scheduled emails for booking {}", cancelled, bookingId);
    }

    /**
     * Process scheduled email queue every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void processScheduledEmails() {
        List<ScheduledEmail> pending = scheduledEmailRepo.findPendingEmails(OffsetDateTime.now());
        if (pending.isEmpty()) return;

        log.info("Processing {} scheduled emails", pending.size());

        for (ScheduledEmail scheduled : pending) {
            try {
                processScheduledEmail(scheduled);
                scheduled.setSent(true);
                scheduled.setSentAt(OffsetDateTime.now());
                scheduledEmailRepo.save(scheduled);
            } catch (Exception e) {
                log.error("Failed to process scheduled email {}: {}", scheduled.getId(), e.getMessage());
            }
        }
    }

    private void processScheduledEmail(ScheduledEmail scheduled) {
        String type = scheduled.getEmailType();
        UUID bookingId = scheduled.getBookingId();
        UUID userId = scheduled.getUserId();

        // Fetch booking and user data via existing clients
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId.toString());
        UserClient.UserInfo user = userClient.getUser(userId.toString());

        if (booking == null || user == null) {
            log.warn("Cannot process scheduled email {} — booking or user data unavailable", scheduled.getId());
            return;
        }

        EmailContext ctx = buildContextFromBooking(booking, user);
        String email = user.email();
        UUID hostId = booking.hostId() != null && !booking.hostId().isEmpty() ? UUID.fromString(booking.hostId()) : null;
        UUID listingId = booking.listingId() != null && !booking.listingId().isEmpty() ? UUID.fromString(booking.listingId()) : null;

        int chapter = switch (type) {
            case "CHAPTER_3" -> 3;
            case "CHAPTER_4" -> 4;
            case "CHAPTER_5" -> 5;
            case "CHAPTER_6" -> 6;
            case "CHAPTER_7" -> 7;
            case "CHAPTER_8" -> 8;
            case "CHAPTER_9" -> 9;
            default -> 0;
        };

        if (chapter > 0) {
            journeyChapterService.sendChapter(chapter, ctx, bookingId, userId, hostId, listingId, email);
        }
    }

    private void scheduleEmail(UUID bookingId, UUID userId, String type, OffsetDateTime scheduledFor, String contextJson) {
        ScheduledEmail email = new ScheduledEmail();
        email.setBookingId(bookingId);
        email.setUserId(userId);
        email.setEmailType(type);
        email.setScheduledFor(scheduledFor);
        email.setContextJson(contextJson);
        scheduledEmailRepo.save(email);
    }

    private EmailContext buildContextFromBooking(BookingClient.BookingInfo booking, UserClient.UserInfo user) {
        EmailContext ctx = new EmailContext();
        ctx.setGuestName(booking.guestName());
        ctx.setGuestEmail(user.email());
        ctx.setBookingRef(booking.bookingRef());
        ctx.setBookingUrl(baseUrl + "/dashboard/bookings");
        ctx.setReviewUrl(baseUrl + "/dashboard/bookings");
        ctx.setFeedbackUrl(baseUrl + "/feedback");
        ctx.setUnsubscribeUrl(baseUrl + "/settings/email-preferences");
        return ctx;
    }
}
