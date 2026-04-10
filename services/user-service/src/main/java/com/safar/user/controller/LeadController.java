package com.safar.user.controller;

import com.safar.user.entity.LocalityAlert;
import com.safar.user.entity.PriceAlert;
import com.safar.user.entity.UserLead;
import com.safar.user.repository.UserLeadRepository;
import com.safar.user.service.LeadManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final UserLeadRepository leadRepo;
    private final LeadManagementService leadService;

    // ── Capture Lead (enhanced with scoring + Kafka) ─────────────────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> captureLead(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email is required"));
        }

        UserLead lead = leadService.captureOrUpdateLead(
                email, body.get("name"), body.get("phone"), body.get("city"),
                body.getOrDefault("source", "WEBSITE_POPUP"),
                body.get("utmSource"), body.get("utmMedium"), body.get("utmCampaign"),
                body.get("leadType"),
                "true".equalsIgnoreCase(body.get("whatsappOptin")));

        String status = lead.getCreatedAt() != null && lead.getUpdatedAt() != null
                && lead.getCreatedAt().equals(lead.getUpdatedAt()) ? "created" : "updated";

        return ResponseEntity.ok(Map.of(
                "status", status,
                "leadId", lead.getId().toString(),
                "score", lead.getLeadScore(),
                "segment", lead.getSegment(),
                "message", "created".equals(status) ? "You're in! Watch for exclusive deals." : "Welcome back! You're subscribed."));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("error", "Email required"));

        leadRepo.findByEmail(email.trim().toLowerCase()).ifPresent(lead -> {
            lead.setSubscribed(false);
            leadRepo.save(lead);
        });

        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }

    // ── Activity Tracking (called from frontend) ─────────────────────────

    @PostMapping("/activity")
    public ResponseEntity<Map<String, String>> trackActivity(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String activityType = body.get("type");
        if (activityType == null) return ResponseEntity.badRequest().body(Map.of("error", "Activity type required"));

        leadService.trackActivity(email, activityType, body.get("metadata"));
        return ResponseEntity.ok(Map.of("status", "tracked"));
    }

    // ── Price Alert ──────────────────────────────────────────────────────

    @PostMapping("/price-alert")
    public ResponseEntity<PriceAlert> createPriceAlert(@RequestBody Map<String, Object> body,
                                                        Authentication auth) {
        String email = (String) body.get("email");
        UUID userId = auth != null ? UUID.fromString(auth.getName()) : null;
        UUID listingId = UUID.fromString((String) body.get("listingId"));
        String listingTitle = (String) body.get("listingTitle");
        String listingCity = (String) body.get("listingCity");
        long thresholdPaise = ((Number) body.get("thresholdPaise")).longValue();

        if (email == null && auth != null) {
            email = body.get("email") != null ? (String) body.get("email") : auth.getName() + "@safar.alert";
        }
        if (email == null) return ResponseEntity.badRequest().build();

        PriceAlert alert = leadService.createPriceAlert(email, userId, listingId, listingTitle, listingCity, thresholdPaise);
        return ResponseEntity.ok(alert);
    }

    // ── Locality Alert ───────────────────────────────────────────────────

    @PostMapping("/locality-alert")
    public ResponseEntity<LocalityAlert> createLocalityAlert(@RequestBody Map<String, Object> body,
                                                              Authentication auth) {
        String email = (String) body.get("email");
        UUID userId = auth != null ? UUID.fromString(auth.getName()) : null;
        String city = (String) body.get("city");
        String locality = (String) body.get("locality");
        String listingType = (String) body.get("listingType");
        Long maxPricePaise = body.get("maxPricePaise") != null ? ((Number) body.get("maxPricePaise")).longValue() : null;

        if (email == null) return ResponseEntity.badRequest().build();
        if (city == null) return ResponseEntity.badRequest().build();

        LocalityAlert alert = leadService.createLocalityAlert(email, userId, city, locality, listingType, maxPricePaise);
        return ResponseEntity.ok(alert);
    }

    // ── Host Earning Calculator ──────────────────────────────────────────

    @GetMapping("/host-calculator")
    public ResponseEntity<Map<String, Object>> hostCalculator(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String listingType,
            @RequestParam(defaultValue = "1") int rooms) {
        return ResponseEntity.ok(leadService.calculateHostEarnings(city, listingType, rooms));
    }

    @PostMapping("/host-calculator")
    public ResponseEntity<Map<String, Object>> hostCalculatorCapture(@RequestBody Map<String, String> body) {
        // Capture as host lead + return earnings
        String email = body.get("email");
        if (email != null) {
            leadService.captureOrUpdateLead(email, body.get("name"), body.get("phone"),
                    body.get("city"), "HOST_CALCULATOR", null, null, null, "HOST_PROSPECT", false);
        }
        int rooms = 1;
        try { rooms = Integer.parseInt(body.getOrDefault("rooms", "1")); } catch (Exception ignored) {}
        return ResponseEntity.ok(leadService.calculateHostEarnings(body.get("city"), body.get("listingType"), rooms));
    }
}
