package com.safar.user.service;

import com.safar.user.entity.*;
import com.safar.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadManagementService {

    private final UserLeadRepository leadRepo;
    private final PriceAlertRepository priceAlertRepo;
    private final LocalityAlertRepository localityAlertRepo;
    private final LeadActivityRepository activityRepo;
    private final NurtureCampaignRepository campaignRepo;
    private final KafkaTemplate<String, String> kafka;

    // ── Lead Capture (enhanced) ──────────────────────────────────────────

    @Transactional
    public UserLead captureOrUpdateLead(String email, String name, String phone, String city,
                                         String source, String utmSource, String utmMedium,
                                         String utmCampaign, String leadType, boolean whatsappOptin) {
        String normalizedEmail = email.trim().toLowerCase();
        UserLead lead = leadRepo.findByEmail(normalizedEmail).orElse(null);

        if (lead != null) {
            if (name != null) lead.setName(name);
            if (city != null) lead.setCity(city);
            if (phone != null) lead.setPhone(phone);
            if (whatsappOptin) lead.setWhatsappOptin(true);
            lead.setSubscribed(true);
            lead.setLastActiveAt(OffsetDateTime.now());
            lead = leadRepo.save(lead);
            log.info("Lead updated: {} (score={})", normalizedEmail, lead.getLeadScore());
        } else {
            int demoScore = calculateDemographicScore(city, normalizedEmail);
            lead = UserLead.builder()
                    .email(normalizedEmail)
                    .name(name).phone(phone).city(city)
                    .source(source != null ? source : "WEBSITE_POPUP")
                    .utmSource(utmSource).utmMedium(utmMedium).utmCampaign(utmCampaign)
                    .leadType(leadType != null ? leadType : "GUEST")
                    .whatsappOptin(whatsappOptin)
                    .demographicScore(demoScore)
                    .leadScore(demoScore)
                    .segment("NEW")
                    .lastActiveAt(OffsetDateTime.now())
                    .build();
            lead = leadRepo.save(lead);
            log.info("New lead captured: {} from {} ({})", normalizedEmail, city, source);

            // Publish Kafka event for welcome email
            try {
                kafka.send("lead.captured", lead.getId().toString(),
                        String.format("{\"leadId\":\"%s\",\"email\":\"%s\",\"name\":\"%s\",\"city\":\"%s\",\"source\":\"%s\",\"leadType\":\"%s\"}",
                                lead.getId(), normalizedEmail,
                                name != null ? name : "",
                                city != null ? city : "",
                                lead.getSource(),
                                lead.getLeadType()));
            } catch (Exception e) {
                log.warn("Kafka lead.captured failed: {}", e.getMessage());
            }
        }
        return lead;
    }

    // ── Auto-Conversion (called from profile sync) ───────────────────────

    @Transactional
    public void checkAndConvertLead(UUID userId, String email, String phone) {
        if (email == null && phone == null) return;

        UserLead lead = null;
        if (email != null) {
            lead = leadRepo.findByEmail(email.trim().toLowerCase()).orElse(null);
        }
        if (lead == null && phone != null) {
            // Try phone match (less common but handles phone-first signups)
            lead = leadRepo.findAll().stream()
                    .filter(l -> phone.equals(l.getPhone()) && !Boolean.TRUE.equals(l.getConverted()))
                    .findFirst().orElse(null);
        }

        if (lead != null && !Boolean.TRUE.equals(lead.getConverted())) {
            lead.setConverted(true);
            lead.setConvertedUserId(userId);
            lead.setConvertedAt(OffsetDateTime.now());
            lead.setSegment("CONVERTED");
            leadRepo.save(lead);
            log.info("Lead auto-converted: {} → userId={}", lead.getEmail(), userId);

            try {
                kafka.send("lead.converted", lead.getId().toString(),
                        String.format("{\"leadId\":\"%s\",\"email\":\"%s\",\"userId\":\"%s\",\"source\":\"%s\"}",
                                lead.getId(), lead.getEmail(), userId, lead.getSource()));
            } catch (Exception e) {
                log.warn("Kafka lead.converted failed: {}", e.getMessage());
            }
        }
    }

    // ── Activity Tracking & Scoring ──────────────────────────────────────

    @Transactional
    public void trackActivity(String email, String activityType, String metadata) {
        UserLead lead = email != null ? leadRepo.findByEmail(email.trim().toLowerCase()).orElse(null) : null;

        int scoreDelta = getScoreDelta(activityType);

        LeadActivity activity = LeadActivity.builder()
                .leadId(lead != null ? lead.getId() : null)
                .email(email)
                .activityType(activityType)
                .metadata(metadata)
                .scoreDelta(scoreDelta)
                .build();
        activityRepo.save(activity);

        if (lead != null && !Boolean.TRUE.equals(lead.getConverted())) {
            // Update behavioral counters
            switch (activityType) {
                case "PAGE_VIEW" -> lead.setPagesViewed((lead.getPagesViewed() != null ? lead.getPagesViewed() : 0) + 1);
                case "SEARCH" -> {
                    lead.setSearchesPerformed((lead.getSearchesPerformed() != null ? lead.getSearchesPerformed() : 0) + 1);
                    if (metadata != null) {
                        lead.setLastSearchQuery(metadata.length() > 500 ? metadata.substring(0, 500) : metadata);
                    }
                }
                case "LISTING_VIEW" -> lead.setListingsViewed((lead.getListingsViewed() != null ? lead.getListingsViewed() : 0) + 1);
                case "WISHLIST" -> lead.setWishlistCount((lead.getWishlistCount() != null ? lead.getWishlistCount() : 0) + 1);
                case "CHECKOUT_START" -> lead.setCheckoutAttempted(true);
            }

            // Update scores
            int behavioral = (lead.getPagesViewed() != null ? lead.getPagesViewed() : 0) * 2
                    + (lead.getSearchesPerformed() != null ? lead.getSearchesPerformed() : 0) * 5
                    + (lead.getListingsViewed() != null ? lead.getListingsViewed() : 0) * 3
                    + (lead.getWishlistCount() != null ? lead.getWishlistCount() : 0) * 10
                    + (Boolean.TRUE.equals(lead.getCheckoutAttempted()) ? 50 : 0);
            lead.setBehavioralScore(Math.min(behavioral, 100)); // cap at 100

            int intent = (lead.getSearchesPerformed() != null && lead.getSearchesPerformed() > 0 ? 20 : 0)
                    + (lead.getListingsViewed() != null && lead.getListingsViewed() > 3 ? 20 : 0)
                    + (Boolean.TRUE.equals(lead.getCheckoutAttempted()) ? 50 : 0)
                    + (lead.getWishlistCount() != null && lead.getWishlistCount() > 0 ? 10 : 0);
            lead.setIntentScore(Math.min(intent, 100));

            // Recency: based on last active
            lead.setLastActiveAt(OffsetDateTime.now());
            lead.setRecencyScore(40); // active now = max recency

            // Total score
            lead.setLeadScore(lead.getBehavioralScore() + lead.getIntentScore()
                    + (lead.getDemographicScore() != null ? lead.getDemographicScore() : 0)
                    + lead.getRecencyScore());

            // Auto-segment
            lead.setSegment(calculateSegment(lead));
            leadRepo.save(lead);
        }
    }

    // ── Price Alerts ─────────────────────────────────────────────────────

    @Transactional
    public PriceAlert createPriceAlert(String email, UUID userId, UUID listingId,
                                        String listingTitle, String listingCity, long thresholdPaise) {
        // Also capture as lead if not exists
        if (!leadRepo.existsByEmail(email.trim().toLowerCase())) {
            captureOrUpdateLead(email, null, null, listingCity, "PRICE_ALERT", null, null, null, "GUEST", false);
        }

        PriceAlert alert = PriceAlert.builder()
                .email(email.trim().toLowerCase())
                .userId(userId)
                .listingId(listingId)
                .listingTitle(listingTitle)
                .listingCity(listingCity)
                .thresholdPaise(thresholdPaise)
                .build();
        return priceAlertRepo.save(alert);
    }

    public List<PriceAlert> checkPriceAlerts(UUID listingId, long newPricePaise) {
        List<PriceAlert> triggered = new ArrayList<>();
        for (PriceAlert alert : priceAlertRepo.findByListingIdAndActiveTrue(listingId)) {
            if (newPricePaise <= alert.getThresholdPaise()) {
                alert.setCurrentPricePaise(newPricePaise);
                alert.setTriggeredCount(alert.getTriggeredCount() + 1);
                alert.setLastTriggeredAt(OffsetDateTime.now());
                priceAlertRepo.save(alert);
                triggered.add(alert);
            }
        }
        return triggered;
    }

    // ── Locality Alerts ──────────────────────────────────────────────────

    @Transactional
    public LocalityAlert createLocalityAlert(String email, UUID userId, String city,
                                              String locality, String listingType, Long maxPricePaise) {
        if (!leadRepo.existsByEmail(email.trim().toLowerCase())) {
            captureOrUpdateLead(email, null, null, city, "LOCALITY_ALERT", null, null, null, "GUEST", false);
        }

        LocalityAlert alert = LocalityAlert.builder()
                .email(email.trim().toLowerCase())
                .userId(userId)
                .city(city)
                .locality(locality)
                .listingType(listingType)
                .maxPricePaise(maxPricePaise)
                .build();
        return localityAlertRepo.save(alert);
    }

    public List<LocalityAlert> checkLocalityAlerts(String city, String locality, String listingType, long pricePaise) {
        List<LocalityAlert> alerts;
        if (locality != null && !locality.isBlank()) {
            alerts = localityAlertRepo.findByCityIgnoreCaseAndLocalityIgnoreCaseAndActiveTrue(city, locality);
        } else {
            alerts = localityAlertRepo.findByCityIgnoreCaseAndActiveTrue(city);
        }
        List<LocalityAlert> triggered = new ArrayList<>();
        for (LocalityAlert alert : alerts) {
            if (alert.getMaxPricePaise() != null && pricePaise > alert.getMaxPricePaise()) continue;
            if (alert.getListingType() != null && !alert.getListingType().equalsIgnoreCase(listingType)) continue;
            alert.setTriggeredCount(alert.getTriggeredCount() + 1);
            alert.setLastTriggeredAt(OffsetDateTime.now());
            localityAlertRepo.save(alert);
            triggered.add(alert);
        }
        return triggered;
    }

    // ── Host Earning Calculator ──────────────────────────────────────────

    public Map<String, Object> calculateHostEarnings(String city, String listingType, int rooms) {
        // Average nightly rate by city (simplified lookup)
        long avgNightlyPaise = getAvgNightlyRate(city, listingType);
        double occupancyRate = 0.65; // 65% average
        int daysPerMonth = 30;
        long monthlyGross = Math.round(avgNightlyPaise * occupancyRate * daysPerMonth * rooms);
        long commission = Math.round(monthlyGross * 0.18); // STARTER tier
        long monthlyNet = monthlyGross - commission;
        long yearlyNet = monthlyNet * 12;

        return Map.of(
                "avgNightlyRate", avgNightlyPaise,
                "occupancyRate", occupancyRate,
                "monthlyGross", monthlyGross,
                "commission", commission,
                "commissionRate", "18%",
                "monthlyNet", monthlyNet,
                "yearlyNet", yearlyNet,
                "rooms", rooms,
                "city", city != null ? city : "",
                "listingType", listingType != null ? listingType : "HOME"
        );
    }

    // ── Admin Stats ──────────────────────────────────────────────────────

    public Map<String, Object> getLeadStats() {
        long total = leadRepo.count();
        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Long> bySegment = new LinkedHashMap<>();
        for (Object[] row : leadRepo.countBySegmentGrouped()) {
            bySegment.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> bySource = new LinkedHashMap<>();
        for (Object[] row : leadRepo.countBySourceGrouped()) {
            bySource.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> conversionBySource = new LinkedHashMap<>();
        for (Object[] row : leadRepo.countConvertedBySourceGrouped()) {
            conversionBySource.put((String) row[0], (Long) row[1]);
        }

        return Map.of(
                "totalLeads", total,
                "leadsThisWeek", leadRepo.countByCreatedAtAfter(now.minusDays(7)),
                "leadsThisMonth", leadRepo.countByCreatedAtAfter(now.minusDays(30)),
                "convertedLeads", leadRepo.countByConvertedTrue(),
                "conversionRate", total > 0 ? Math.round(leadRepo.countByConvertedTrue() * 100.0 / total) + "%" : "0%",
                "bySegment", bySegment,
                "bySource", bySource,
                "conversionBySource", conversionBySource,
                "activePriceAlerts", priceAlertRepo.countByActiveTrue(),
                "activeLocalityAlerts", localityAlertRepo.countByActiveTrue()
        );
    }

    // ── Scoring Helpers ──────────────────────────────────────────────────

    private int calculateDemographicScore(String city, String email) {
        int score = 0;
        // Tier 1 cities
        if (city != null) {
            String c = city.toLowerCase();
            if (List.of("mumbai", "delhi", "bangalore", "bengaluru", "hyderabad", "chennai", "kolkata", "pune").contains(c)) {
                score += 20;
            } else if (List.of("ahmedabad", "jaipur", "lucknow", "chandigarh", "goa", "kochi", "gurgaon", "noida").contains(c)) {
                score += 15;
            } else {
                score += 10;
            }
        }
        // Corporate email domain
        if (email != null && !email.endsWith("@gmail.com") && !email.endsWith("@yahoo.com")
                && !email.endsWith("@hotmail.com") && !email.endsWith("@outlook.com")) {
            score += 15; // likely corporate
        }
        return score;
    }

    private String calculateSegment(UserLead lead) {
        if (Boolean.TRUE.equals(lead.getConverted())) return "CONVERTED";
        if ("HOST_PROSPECT".equals(lead.getLeadType())) return "HOST_PROSPECT";
        int score = lead.getLeadScore() != null ? lead.getLeadScore() : 0;
        if (score >= 80) return "HOT";
        if (score >= 40) return "WARM";
        if (lead.getLastActiveAt() != null && lead.getLastActiveAt().isBefore(OffsetDateTime.now().minusDays(30))) return "COLD";
        return "NEW";
    }

    private int getScoreDelta(String activityType) {
        return switch (activityType) {
            case "PAGE_VIEW" -> 2;
            case "SEARCH" -> 5;
            case "LISTING_VIEW" -> 3;
            case "WISHLIST" -> 10;
            case "CHECKOUT_START" -> 50;
            case "EMAIL_OPEN" -> 5;
            case "EMAIL_CLICK" -> 15;
            case "PRICE_ALERT_SET" -> 20;
            case "LOCALITY_ALERT_SET" -> 15;
            default -> 1;
        };
    }

    private long getAvgNightlyRate(String city, String listingType) {
        if (city == null) return 250000; // ₹2,500 default
        String c = city.toLowerCase();
        boolean isPg = "PG".equalsIgnoreCase(listingType) || "CO_LIVING".equalsIgnoreCase(listingType);
        if (isPg) {
            return List.of("mumbai", "bangalore", "bengaluru", "delhi", "gurgaon").contains(c) ? 1200000 : 800000;
        }
        return switch (c) {
            case "mumbai" -> 450000;
            case "goa" -> 350000;
            case "delhi", "gurgaon", "noida" -> 400000;
            case "bangalore", "bengaluru" -> 350000;
            case "hyderabad" -> 300000;
            case "jaipur", "udaipur" -> 250000;
            default -> 200000;
        };
    }
}
