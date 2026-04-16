package com.safar.chef.controller;

import com.safar.chef.entity.ChefBooking;
import com.safar.chef.service.LiveTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-bookings")
@RequiredArgsConstructor
public class LiveTrackingController {

    private final LiveTrackingService trackingService;

    @PostMapping("/{id}/location")
    public ResponseEntity<ChefBooking> updateLocation(Authentication auth,
                                                       @PathVariable UUID id,
                                                       @RequestParam Double lat,
                                                       @RequestParam Double lng,
                                                       @RequestParam(required = false) Integer etaMinutes) {
        UUID chefUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(trackingService.updateLocation(chefUserId, id, lat, lng, etaMinutes));
    }

    /**
     * Share live location + send LOCATION message in chef-customer chat.
     */
    @PostMapping("/{id}/share-location")
    public ResponseEntity<ChefBooking> shareLocationInChat(Authentication auth,
                                                            @PathVariable UUID id,
                                                            @RequestParam Double lat,
                                                            @RequestParam Double lng,
                                                            @RequestParam(required = false) Integer etaMinutes) {
        UUID chefUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(trackingService.shareLocationInChat(chefUserId, id, lat, lng, etaMinutes));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<Map<String, Object>> getTracking(@PathVariable UUID id) {
        return ResponseEntity.ok(trackingService.getTrackingInfo(id));
    }
}
