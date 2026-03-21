package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import com.safar.notification.entity.EmailChapter;
import com.safar.notification.repository.EmailChapterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class JourneyChapterService {

    private static final Logger log = LoggerFactory.getLogger(JourneyChapterService.class);

    public static final Map<Integer, String> CHAPTER_NAMES = Map.of(
            1, "Journey Unlocked",
            2, "You're All Set",
            3, "The Countdown Begins",
            4, "Your Stay Itinerary",
            5, "Today's the Day",
            6, "How's Everything?",
            7, "Until Next Time",
            8, "Your Story Matters",
            9, "Missing You"
    );

    public static final Map<Integer, String> CHAPTER_TEMPLATES = Map.of(
            1, "booking-confirmed",
            2, "payment-receipt",
            3, "pre-arrival-7day",
            4, "pre-arrival-48hr",
            5, "checkin-day",
            6, "mid-stay-check",
            7, "checkout-day",
            8, "post-stay-review",
            9, "re-engagement"
    );

    private final EmailChapterRepository chapterRepository;
    private final EmailTemplateService emailTemplateService;
    private final ToneService toneService;

    public JourneyChapterService(EmailChapterRepository chapterRepository,
                                  EmailTemplateService emailTemplateService,
                                  ToneService toneService) {
        this.chapterRepository = chapterRepository;
        this.emailTemplateService = emailTemplateService;
        this.toneService = toneService;
    }

    public boolean hasChapterBeenSent(UUID bookingId, int chapter) {
        return chapterRepository.existsByBookingIdAndChapterNumber(bookingId, chapter);
    }

    public void sendChapter(int chapterNum, EmailContext ctx, UUID bookingId, UUID guestId, UUID hostId, UUID listingId, String email) {
        if (hasChapterBeenSent(bookingId, chapterNum)) {
            log.info("Chapter {} already sent for booking {}", chapterNum, bookingId);
            return;
        }

        String chapterName = CHAPTER_NAMES.getOrDefault(chapterNum, "Update");
        String template = CHAPTER_TEMPLATES.getOrDefault(chapterNum, "generic");

        ctx.setChapterNumber(chapterNum);
        ctx.setChapterTitle(chapterName);
        ctx.setTotalChapters(9);

        String subject = emailTemplateService.buildChapterSubject(
                chapterNum, ctx.getListingCity() != null ? ctx.getListingCity() : "Safar", chapterName
        );

        emailTemplateService.sendHtmlEmail(email, subject, template, ctx);

        // Record chapter sent
        EmailChapter record = new EmailChapter();
        record.setBookingId(bookingId);
        record.setGuestId(guestId);
        record.setHostId(hostId);
        record.setListingId(listingId);
        record.setChapterNumber(chapterNum);
        record.setChapterName(chapterName);
        record.setEmailTo(email);
        record.setTone(ctx.getTone());
        record.setSentAt(OffsetDateTime.now());
        chapterRepository.save(record);

        log.info("Sent chapter {} [{}] for booking {} to {}", chapterNum, chapterName, bookingId, email);
    }
}
