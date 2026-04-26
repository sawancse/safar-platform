package com.safar.services.controller;

import com.safar.services.entity.ChefReferral;
import com.safar.services.service.ChefReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs/referrals")
@RequiredArgsConstructor
public class ChefReferralController {

    private final ChefReferralService referralService;

    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, String>> generateCode(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String code = referralService.generateReferralCode(userId);
        return ResponseEntity.ok(Map.of("referralCode", code));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChefReferral>> getMyReferrals(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(referralService.getMyReferrals(userId));
    }
}
