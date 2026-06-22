package com.safar.booking.controller;

import com.safar.booking.dto.CouponResponse;
import com.safar.booking.dto.CouponValidationResult;
import com.safar.booking.dto.CreateCouponRequest;
import com.safar.booking.dto.ValidateCouponRequest;
import com.safar.booking.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** Public: preview a coupon against a cart. Auth optional (used for per-user limit). */
    @PostMapping("/validate")
    public CouponValidationResult validate(Authentication auth, @RequestBody ValidateCouponRequest req) {
        UUID userId = auth != null ? UUID.fromString(auth.getName()) : null;
        return couponService.validate(req.code(), req.listingId(), userId, req.subtotalPaise());
    }

    // ── Admin (platform coupons) ───────────────────────────────────────────
    @PostMapping("/admin")
    public CouponResponse adminCreate(Authentication auth, @RequestBody CreateCouponRequest req) {
        requireAdmin(auth);
        return CouponResponse.from(couponService.create(req, UUID.fromString(auth.getName()), false));
    }

    @GetMapping("/admin")
    public List<CouponResponse> adminList(Authentication auth) {
        requireAdmin(auth);
        return couponService.listAll().stream().map(CouponResponse::from).toList();
    }

    @PostMapping("/admin/{id}/active")
    public CouponResponse adminSetActive(Authentication auth, @PathVariable UUID id, @RequestParam boolean active) {
        requireAdmin(auth);
        return CouponResponse.from(couponService.setActive(id, active, UUID.fromString(auth.getName()), true));
    }

    // ── Host (per-listing coupons) ─────────────────────────────────────────
    @PostMapping("/host")
    public CouponResponse hostCreate(Authentication auth, @RequestBody CreateCouponRequest req) {
        return CouponResponse.from(couponService.create(req, UUID.fromString(auth.getName()), true));
    }

    @GetMapping("/host")
    public List<CouponResponse> hostList(Authentication auth) {
        return couponService.listForOwner(UUID.fromString(auth.getName())).stream().map(CouponResponse::from).toList();
    }

    @PostMapping("/host/{id}/active")
    public CouponResponse hostSetActive(Authentication auth, @PathVariable UUID id, @RequestParam boolean active) {
        return CouponResponse.from(couponService.setActive(id, active, UUID.fromString(auth.getName()), false));
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
