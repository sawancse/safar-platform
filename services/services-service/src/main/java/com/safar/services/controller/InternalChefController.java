package com.safar.services.controller;

import com.safar.services.service.ChefProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Internal (service-to-service) chef endpoints. Gateway exposes /api/v1/internal/**
 * as public; in prod these paths are only reachable inside the VPC.
 */
@RestController
@RequestMapping("/api/v1/internal/chefs")
@RequiredArgsConstructor
public class InternalChefController {

    private final ChefProfileService chefProfileService;

    /** Repoint chef_profiles.user_id from loserId to keeperId during guest account merge. */
    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> merge(@RequestBody Map<String, String> body) {
        UUID keeperId = UUID.fromString(body.get("keeperId"));
        UUID loserId = UUID.fromString(body.get("loserId"));
        int moved = chefProfileService.mergeUserId(keeperId, loserId);
        return ResponseEntity.ok(Map.of(
                "keeperId", keeperId.toString(),
                "loserId", loserId.toString(),
                "chefProfilesMoved", moved));
    }
}
