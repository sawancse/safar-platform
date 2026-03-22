package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import com.safar.notification.entity.FestivalCampaign;
import com.safar.notification.repository.FestivalCampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FestivalService {

    private static final Logger log = LoggerFactory.getLogger(FestivalService.class);

    // Production: set NOTIFICATION_BASE_URL=https://ysafar.com
    @Value("${notification.base-url}")
    private String baseUrl;

    private final FestivalCampaignRepository festivalRepo;
    private final EmailTemplateService emailTemplateService;

    public FestivalService(FestivalCampaignRepository festivalRepo,
                           EmailTemplateService emailTemplateService) {
        this.festivalRepo = festivalRepo;
        this.emailTemplateService = emailTemplateService;
    }

    public List<FestivalCampaign> getUpcomingFestivals(int daysAhead) {
        LocalDate today = LocalDate.now();
        return festivalRepo.findByFestivalDateBetweenAndIsActiveTrue(today, today.plusDays(daysAhead));
    }

    public List<FestivalCampaign> getTodaysFestivals() {
        return festivalRepo.findByFestivalDateAndIsActiveTrue(LocalDate.now());
    }

    public List<FestivalCampaign> getFestivalsForRegion(String region) {
        return festivalRepo.findActiveByDateAndRegion(LocalDate.now().plusDays(7), region);
    }

    public void sendFestivalCampaign(FestivalCampaign festival, String guestEmail, String guestName, String guestLanguage) {
        // Match language/region
        if (festival.getLanguageCode() != null && guestLanguage != null
                && !festival.getLanguageCode().equals(guestLanguage) && !"en".equals(festival.getLanguageCode())) {
            return; // Skip if language-specific festival doesn't match guest
        }

        EmailContext ctx = new EmailContext();
        ctx.setGuestName(guestName);
        ctx.setFestivalName(festival.getFestivalName());
        ctx.setFestivalHeadline(festival.getCampaignHeadline());
        ctx.setFestivalBody(festival.getCampaignBody());
        ctx.setDiscoveryCategories(festival.getDiscoveryCategories());
        ctx.setUnsubscribeUrl(baseUrl + "/settings/email-preferences");
        ctx.setDashboardUrl(baseUrl + "/search");

        emailTemplateService.sendHtmlEmail(
                guestEmail,
                festival.getCampaignSubject(),
                "festival-campaign",
                ctx
        );

        log.info("Sent festival campaign [{}] to {}", festival.getFestivalName(), guestEmail);
    }
}
