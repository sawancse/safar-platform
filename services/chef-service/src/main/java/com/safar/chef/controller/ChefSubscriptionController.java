package com.safar.chef.controller;

import com.safar.chef.dto.CreateSubscriptionRequest;
import com.safar.chef.dto.ModifySubscriptionRequest;
import com.safar.chef.entity.ChefSubscription;
import com.safar.chef.service.ChefSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-subscriptions")
@RequiredArgsConstructor
public class ChefSubscriptionController {

    private final ChefSubscriptionService subscriptionService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<Page<ChefSubscription>> adminListAll(Pageable pageable, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(subscriptionService.adminListAll(pageable));
    }

    @PostMapping
    public ResponseEntity<ChefSubscription> create(Authentication auth,
                                                    @RequestBody CreateSubscriptionRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(customerId, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ChefSubscription> cancel(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId, id, reason));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChefSubscription> modify(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestBody ModifySubscriptionRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(subscriptionService.modifySubscription(customerId, id, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChefSubscription>> getMySubscriptions(Authentication auth) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(customerId));
    }

    @GetMapping("/chef")
    public ResponseEntity<List<ChefSubscription>> getChefSubscriptions(Authentication auth) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(subscriptionService.getChefSubscriptions(chefId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChefSubscription> get(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.getSubscription(id));
    }
}
