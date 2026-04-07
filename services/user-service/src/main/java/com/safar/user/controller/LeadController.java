package com.safar.user.controller;

import com.safar.user.entity.UserLead;
import com.safar.user.repository.UserLeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final UserLeadRepository leadRepo;

    @PostMapping
    public ResponseEntity<Map<String, Object>> captureLead(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email is required"));
        }

        // Upsert — if email exists, update city/name but don't duplicate
        var existing = leadRepo.findByEmail(email.trim().toLowerCase());
        if (existing.isPresent()) {
            UserLead lead = existing.get();
            if (body.get("name") != null) lead.setName(body.get("name"));
            if (body.get("city") != null) lead.setCity(body.get("city"));
            lead.setSubscribed(true);
            leadRepo.save(lead);
            return ResponseEntity.ok(Map.of("status", "updated", "message", "Welcome back! You're subscribed for deals."));
        }

        UserLead lead = UserLead.builder()
                .email(email.trim().toLowerCase())
                .name(body.get("name"))
                .phone(body.get("phone"))
                .city(body.get("city"))
                .source(body.getOrDefault("source", "WEBSITE_POPUP"))
                .utmSource(body.get("utmSource"))
                .utmMedium(body.get("utmMedium"))
                .utmCampaign(body.get("utmCampaign"))
                .build();

        leadRepo.save(lead);
        log.info("New lead captured: {} from {} ({})", email, body.get("city"), body.get("source"));

        return ResponseEntity.ok(Map.of("status", "created", "message", "You're in! Watch for exclusive deals."));
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
}
