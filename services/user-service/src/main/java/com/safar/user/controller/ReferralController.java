package com.safar.user.controller;

import com.safar.user.entity.Referral;
import com.safar.user.entity.enums.ReferralType;
import com.safar.user.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/my-code")
    public ResponseEntity<Map<String, String>> getMyCode(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(Map.of("code", referralService.getOrCreateReferralCode(userId)));
    }

    @PostMapping("/apply")
    public ResponseEntity<Referral> applyCode(
            @RequestParam String code,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(referralService.applyReferralCode(code, userId));
    }

    @PostMapping("/create")
    public ResponseEntity<Referral> createReferral(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "GUEST") ReferralType type) {
        return ResponseEntity.ok(referralService.createReferral(userId, type));
    }

    @GetMapping("/my-referrals")
    public ResponseEntity<List<Referral>> getMyReferrals(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(referralService.getUserReferrals(userId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(referralService.getReferralStats(userId));
    }
}
