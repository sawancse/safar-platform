package com.safar.user.scheduler;

import com.safar.user.entity.UserLead;
import com.safar.user.repository.UserLeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled tasks for lead nurturing:
 * 1. Welcome drip: Day 0 (immediate), Day 3, Day 7
 * 2. Score decay: reduce recency score for inactive leads
 * 3. Re-engagement: send deals to cold leads (30+ days inactive)
 * 4. Segment refresh: recalculate segments based on scores
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadNurtureScheduler {

    private final UserLeadRepository leadRepo;
    private final KafkaTemplate<String, String> kafka;

    // ── Welcome Drip: Day 0 (runs every 10 min) ─────────────────────────
    @Scheduled(fixedRate = 600000) // 10 min
    @Transactional
    public void sendWelcomeDay0() {
        List<UserLead> leads = leadRepo.findByNurtureDay0SentFalseAndSubscribedTrueAndConvertedFalse();
        for (UserLead lead : leads) {
            try {
                kafka.send("lead.nurture.welcome", lead.getId().toString(),
                        buildNurtureJson(lead, "DAY0", "Welcome to Safar! Your journey begins here"));
                lead.setNurtureDay0Sent(true);
                lead.setNurtureStage("WELCOME_DAY0");
                leadRepo.save(lead);
                log.debug("Welcome Day 0 sent to {}", lead.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send welcome Day 0 to {}: {}", lead.getEmail(), e.getMessage());
            }
        }
        if (!leads.isEmpty()) log.info("Welcome Day 0 sent to {} leads", leads.size());
    }

    // ── Welcome Drip: Day 3 (runs every hour) ────────────────────────────
    @Scheduled(cron = "0 0 * * * *") // every hour
    @Transactional
    public void sendWelcomeDay3() {
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minusHours(72);
        List<UserLead> leads = leadRepo.findByNurtureDay3SentFalseAndNurtureDay0SentTrueAndSubscribedTrueAndConvertedFalseAndCreatedAtBefore(threeDaysAgo);
        for (UserLead lead : leads) {
            try {
                String city = lead.getCity() != null ? lead.getCity() : "India";
                kafka.send("lead.nurture.day3", lead.getId().toString(),
                        buildNurtureJson(lead, "DAY3", "Top stays in " + city + " waiting for you"));
                lead.setNurtureDay3Sent(true);
                lead.setNurtureStage("WELCOME_DAY3");
                leadRepo.save(lead);
            } catch (Exception e) {
                log.warn("Failed to send Day 3 to {}: {}", lead.getEmail(), e.getMessage());
            }
        }
        if (!leads.isEmpty()) log.info("Welcome Day 3 sent to {} leads", leads.size());
    }

    // ── Welcome Drip: Day 7 Offer (runs every hour) ──────────────────────
    @Scheduled(cron = "0 30 * * * *") // every hour at :30
    @Transactional
    public void sendWelcomeDay7() {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusHours(168);
        List<UserLead> leads = leadRepo.findByNurtureDay7SentFalseAndNurtureDay3SentTrueAndSubscribedTrueAndConvertedFalseAndCreatedAtBefore(sevenDaysAgo);
        for (UserLead lead : leads) {
            try {
                kafka.send("lead.nurture.day7", lead.getId().toString(),
                        buildNurtureJson(lead, "DAY7", "₹500 off your first stay — expires in 48 hours!"));
                lead.setNurtureDay7Sent(true);
                lead.setNurtureStage("WELCOME_DAY7");
                leadRepo.save(lead);
            } catch (Exception e) {
                log.warn("Failed to send Day 7 to {}: {}", lead.getEmail(), e.getMessage());
            }
        }
        if (!leads.isEmpty()) log.info("Welcome Day 7 sent to {} leads", leads.size());
    }

    // ── Score Decay: reduce recency for inactive leads (runs daily at 2 AM)
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void decayScores() {
        List<UserLead> all = leadRepo.findAll();
        int updated = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (UserLead lead : all) {
            if (Boolean.TRUE.equals(lead.getConverted())) continue;
            if (lead.getLastActiveAt() == null) continue;

            long daysSinceActive = java.time.Duration.between(lead.getLastActiveAt(), now).toDays();
            int newRecency;
            if (daysSinceActive <= 1) newRecency = 40;
            else if (daysSinceActive <= 7) newRecency = 30;
            else if (daysSinceActive <= 14) newRecency = 20;
            else if (daysSinceActive <= 30) newRecency = 10;
            else newRecency = 0;

            if (!Integer.valueOf(newRecency).equals(lead.getRecencyScore())) {
                lead.setRecencyScore(newRecency);
                lead.setLeadScore(
                        (lead.getBehavioralScore() != null ? lead.getBehavioralScore() : 0)
                        + (lead.getIntentScore() != null ? lead.getIntentScore() : 0)
                        + (lead.getDemographicScore() != null ? lead.getDemographicScore() : 0)
                        + newRecency);

                // Recalculate segment
                String oldSegment = lead.getSegment();
                String newSegment = recalcSegment(lead);
                if (!newSegment.equals(oldSegment)) {
                    lead.setSegment(newSegment);
                }
                leadRepo.save(lead);
                updated++;
            }
        }
        if (updated > 0) log.info("Score decay: updated {} leads", updated);
    }

    // ── Re-engagement: cold leads (runs daily at 10 AM) ──────────────────
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendReEngagement() {
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        List<UserLead> coldLeads = leadRepo.findBySubscribedTrueAndConvertedFalseAndLastActiveAtBefore(thirtyDaysAgo);

        int sent = 0;
        for (UserLead lead : coldLeads) {
            if ("COLD".equals(lead.getSegment()) || "NEW".equals(lead.getSegment())) {
                try {
                    String city = lead.getLastSearchCity() != null ? lead.getLastSearchCity()
                            : (lead.getCity() != null ? lead.getCity() : "India");
                    kafka.send("lead.nurture.re-engagement", lead.getId().toString(),
                            buildNurtureJson(lead, "RE_ENGAGEMENT", "We miss you! New stays in " + city));
                    lead.setSegment("COLD");
                    leadRepo.save(lead);
                    sent++;
                } catch (Exception e) {
                    log.warn("Re-engagement failed for {}: {}", lead.getEmail(), e.getMessage());
                }
            }
        }
        if (sent > 0) log.info("Re-engagement sent to {} cold leads", sent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String buildNurtureJson(UserLead lead, String stage, String subject) {
        return String.format(
                "{\"leadId\":\"%s\",\"email\":\"%s\",\"name\":\"%s\",\"city\":\"%s\","
                + "\"stage\":\"%s\",\"subject\":\"%s\",\"leadType\":\"%s\","
                + "\"source\":\"%s\",\"score\":%d}",
                lead.getId(), lead.getEmail(),
                lead.getName() != null ? lead.getName() : "",
                lead.getCity() != null ? lead.getCity() : "",
                stage, subject,
                lead.getLeadType() != null ? lead.getLeadType() : "GUEST",
                lead.getSource() != null ? lead.getSource() : "",
                lead.getLeadScore() != null ? lead.getLeadScore() : 0);
    }

    private String recalcSegment(UserLead lead) {
        if (Boolean.TRUE.equals(lead.getConverted())) return "CONVERTED";
        if ("HOST_PROSPECT".equals(lead.getLeadType())) return "HOST_PROSPECT";
        int score = lead.getLeadScore() != null ? lead.getLeadScore() : 0;
        if (score >= 80) return "HOT";
        if (score >= 40) return "WARM";
        if (lead.getLastActiveAt() != null && lead.getLastActiveAt().isBefore(OffsetDateTime.now().minusDays(30))) return "COLD";
        return "NEW";
    }
}
