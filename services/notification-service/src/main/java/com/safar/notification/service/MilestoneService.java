package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import com.safar.notification.entity.GuestMilestone;
import com.safar.notification.entity.HostMilestone;
import com.safar.notification.repository.GuestMilestoneRepository;
import com.safar.notification.repository.HostMilestoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class MilestoneService {

    private static final Logger log = LoggerFactory.getLogger(MilestoneService.class);

    // Production: set NOTIFICATION_BASE_URL=https://safar.com
    @Value("${notification.base-url}")
    private String baseUrl;

    // Guest milestones: cities explored
    private static final int[] CITY_MILESTONES = {3, 5, 10, 15, 25, 50};
    // Guest milestones: total stays
    private static final int[] STAY_MILESTONES = {5, 10, 25, 50, 100};
    // Guest milestones: money saved (in rupees)
    private static final int[] SAVINGS_MILESTONES = {1000, 5000, 10000, 25000, 50000};
    // Host milestones: consecutive 5-star reviews
    private static final int[] REVIEW_STREAK_MILESTONES = {5, 10, 25, 50};
    // Host milestones: total bookings hosted
    private static final int[] HOSTED_MILESTONES = {10, 25, 50, 100, 250, 500};
    // Host milestones: response rate percentage
    private static final int[] RESPONSE_RATE_MILESTONES = {90, 95, 99};

    private static final Map<String, String> GUEST_MILESTONE_BADGES = Map.of(
            "CITIES_3", "Explorer",
            "CITIES_5", "Wanderer",
            "CITIES_10", "Voyager",
            "CITIES_25", "Globetrotter",
            "STAYS_10", "Safar Regular",
            "STAYS_25", "Safar Loyalist",
            "STAYS_50", "Safar Legend"
    );

    private static final Map<String, String> HOST_MILESTONE_BADGES = Map.of(
            "REVIEWS_10", "Guest Favorite",
            "REVIEWS_25", "Hospitality Pro",
            "HOSTED_50", "Safar Veteran",
            "HOSTED_100", "Safar Champion",
            "RESPONSE_95", "Quick Responder",
            "RESPONSE_99", "Lightning Host"
    );

    private final GuestMilestoneRepository guestMilestoneRepo;
    private final HostMilestoneRepository hostMilestoneRepo;
    private final EmailTemplateService emailTemplateService;

    public MilestoneService(GuestMilestoneRepository guestMilestoneRepo,
                            HostMilestoneRepository hostMilestoneRepo,
                            EmailTemplateService emailTemplateService) {
        this.guestMilestoneRepo = guestMilestoneRepo;
        this.hostMilestoneRepo = hostMilestoneRepo;
        this.emailTemplateService = emailTemplateService;
    }

    public void checkGuestCityMilestone(UUID guestId, String guestEmail, String guestName, int citiesExplored) {
        for (int milestone : CITY_MILESTONES) {
            if (citiesExplored >= milestone && !guestMilestoneRepo.existsByGuestIdAndMilestoneTypeAndMilestoneValue(guestId, "CITIES", milestone)) {
                GuestMilestone m = new GuestMilestone();
                m.setGuestId(guestId);
                m.setMilestoneType("CITIES");
                m.setMilestoneValue(milestone);
                guestMilestoneRepo.save(m);

                String badge = GUEST_MILESTONE_BADGES.getOrDefault("CITIES_" + milestone, "Explorer");
                int nextMilestone = findNextMilestone(CITY_MILESTONES, milestone);
                sendGuestMilestoneEmail(guestEmail, guestName,
                        "You've explored " + milestone + " cities with Safar!",
                        "Your wanderlust is inspiring! You've now visited " + milestone + " different cities through Safar.",
                        badge, milestone, nextMilestone);

                log.info("Guest {} achieved CITIES milestone: {}", guestId, milestone);
            }
        }
    }

    public void checkGuestStayMilestone(UUID guestId, String guestEmail, String guestName, int totalStays) {
        for (int milestone : STAY_MILESTONES) {
            if (totalStays >= milestone && !guestMilestoneRepo.existsByGuestIdAndMilestoneTypeAndMilestoneValue(guestId, "STAYS", milestone)) {
                GuestMilestone m = new GuestMilestone();
                m.setGuestId(guestId);
                m.setMilestoneType("STAYS");
                m.setMilestoneValue(milestone);
                guestMilestoneRepo.save(m);

                String badge = GUEST_MILESTONE_BADGES.getOrDefault("STAYS_" + milestone, "Traveller");
                int nextMilestone = findNextMilestone(STAY_MILESTONES, milestone);
                sendGuestMilestoneEmail(guestEmail, guestName,
                        milestone + " stays completed — you're a " + badge + "!",
                        "From your first booking to stay #" + milestone + ", every journey tells a story.",
                        badge, milestone, nextMilestone);

                log.info("Guest {} achieved STAYS milestone: {}", guestId, milestone);
            }
        }
    }

    public void checkHostReviewStreakMilestone(UUID hostId, String hostEmail, String hostName, int consecutiveFiveStars) {
        for (int milestone : REVIEW_STREAK_MILESTONES) {
            if (consecutiveFiveStars >= milestone && !hostMilestoneRepo.existsByHostIdAndMilestoneTypeAndMilestoneValue(hostId, "REVIEW_STREAK", milestone)) {
                HostMilestone m = new HostMilestone();
                m.setHostId(hostId);
                m.setMilestoneType("REVIEW_STREAK");
                m.setMilestoneValue(milestone);
                hostMilestoneRepo.save(m);

                String badge = HOST_MILESTONE_BADGES.getOrDefault("REVIEWS_" + milestone, "Top Host");
                int nextMilestone = findNextMilestone(REVIEW_STREAK_MILESTONES, milestone);
                sendHostMilestoneEmail(hostEmail, hostName,
                        milestone + " consecutive 5-star reviews!",
                        "Your guests love you! " + milestone + " perfect reviews in a row is incredible hospitality.",
                        badge, milestone, nextMilestone);

                log.info("Host {} achieved REVIEW_STREAK milestone: {}", hostId, milestone);
            }
        }
    }

    public void checkHostBookingMilestone(UUID hostId, String hostEmail, String hostName, int totalHosted) {
        for (int milestone : HOSTED_MILESTONES) {
            if (totalHosted >= milestone && !hostMilestoneRepo.existsByHostIdAndMilestoneTypeAndMilestoneValue(hostId, "HOSTED", milestone)) {
                HostMilestone m = new HostMilestone();
                m.setHostId(hostId);
                m.setMilestoneType("HOSTED");
                m.setMilestoneValue(milestone);
                hostMilestoneRepo.save(m);

                String badge = HOST_MILESTONE_BADGES.getOrDefault("HOSTED_" + milestone, "Experienced Host");
                int nextMilestone = findNextMilestone(HOSTED_MILESTONES, milestone);
                sendHostMilestoneEmail(hostEmail, hostName,
                        milestone + " guests hosted — you're a " + badge + "!",
                        "You've welcomed " + milestone + " guests into your space. That's " + milestone + " stories, " + milestone + " memories.",
                        badge, milestone, nextMilestone);

                log.info("Host {} achieved HOSTED milestone: {}", hostId, milestone);
            }
        }
    }

    private void sendGuestMilestoneEmail(String email, String name, String title, String description, String badge, int value, int nextValue) {
        EmailContext ctx = new EmailContext();
        ctx.setGuestName(name);
        ctx.setMilestoneTitle(title);
        ctx.setMilestoneDescription(description);
        ctx.setMilestoneBadge(badge);
        ctx.setMilestoneValue(value);
        ctx.setNextMilestoneValue(nextValue);
        ctx.setDashboardUrl(baseUrl + "/dashboard");
        ctx.setUnsubscribeUrl(baseUrl + "/settings/email-preferences");
        emailTemplateService.sendHtmlEmail(email, "Achievement Unlocked: " + title, "milestone-guest", ctx);
    }

    private void sendHostMilestoneEmail(String email, String name, String title, String description, String badge, int value, int nextValue) {
        EmailContext ctx = new EmailContext();
        ctx.setHostName(name);
        ctx.setMilestoneTitle(title);
        ctx.setMilestoneDescription(description);
        ctx.setMilestoneBadge(badge);
        ctx.setMilestoneValue(value);
        ctx.setNextMilestoneValue(nextValue);
        ctx.setDashboardUrl(baseUrl + "/host/dashboard");
        ctx.setUnsubscribeUrl(baseUrl + "/settings/email-preferences");
        emailTemplateService.sendHtmlEmail(email, "Host Achievement: " + title, "milestone-host", ctx);
    }

    private int findNextMilestone(int[] milestones, int current) {
        for (int m : milestones) {
            if (m > current) return m;
        }
        return 0; // No next milestone
    }
}
