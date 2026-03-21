package com.safar.user.controller;

import com.safar.user.dto.DiscoveryFeedResponse;
import com.safar.user.service.DiscoveryFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DiscoveryFeedController {

    private final DiscoveryFeedService discoveryFeedService;

    @GetMapping("/feed")
    public ResponseEntity<DiscoveryFeedResponse> getFeed(Authentication auth,
                                                          @RequestParam(required = false) String city,
                                                          @RequestParam(required = false) String lang) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(discoveryFeedService.generateFeed(guestId, city));
    }
}
