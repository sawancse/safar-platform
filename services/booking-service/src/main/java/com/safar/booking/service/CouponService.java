package com.safar.booking.service;

import com.safar.booking.dto.CouponValidationResult;
import com.safar.booking.dto.CreateCouponRequest;
import com.safar.booking.entity.Coupon;
import com.safar.booking.entity.CouponRedemption;
import com.safar.booking.repository.CouponRedemptionRepository;
import com.safar.booking.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Coupon / promo-code engine (MakeMyTrip style). Supports PLATFORM-wide admin
 * coupons and HOST per-listing coupons. {@link #validate} previews the discount;
 * {@link #redeem} books a redemption on payment confirmation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepo;
    private final CouponRedemptionRepository redemptionRepo;
    private final ListingServiceClient listingClient;

    /** Validate a coupon against a cart and compute the discount (no side effects). */
    public CouponValidationResult validate(String code, UUID listingId, UUID userId, long subtotalPaise) {
        if (code == null || code.isBlank()) return CouponValidationResult.invalid(code, "Enter a coupon code");
        Coupon c = couponRepo.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (c == null || !Boolean.TRUE.equals(c.getActive())) {
            return CouponValidationResult.invalid(code, "Invalid or expired coupon");
        }
        LocalDate today = LocalDate.now();
        if (c.getValidFrom() != null && today.isBefore(c.getValidFrom())) {
            return CouponValidationResult.invalid(code, "This coupon isn't active yet");
        }
        if (c.getValidUntil() != null && today.isAfter(c.getValidUntil())) {
            return CouponValidationResult.invalid(code, "This coupon has expired");
        }
        if (c.getUsageLimit() != null && c.getUsedCount() != null && c.getUsedCount() >= c.getUsageLimit()) {
            return CouponValidationResult.invalid(code, "This coupon has reached its usage limit");
        }
        // Scope: HOST coupons only apply to that host's listings (and an optional single listing);
        // PLATFORM coupons may optionally be restricted to one listing.
        if ("HOST".equals(c.getScope())) {
            UUID hostId = null;
            try { hostId = listingClient.getHostId(listingId); } catch (Exception ignored) { /* fail closed */ }
            if (hostId == null || !hostId.equals(c.getOwnerId())
                    || (c.getListingId() != null && !c.getListingId().equals(listingId))) {
                return CouponValidationResult.invalid(code, "This coupon doesn't apply to this property");
            }
        } else if (c.getListingId() != null && !c.getListingId().equals(listingId)) {
            return CouponValidationResult.invalid(code, "This coupon doesn't apply to this property");
        }
        long minBooking = c.getMinBookingPaise() != null ? c.getMinBookingPaise() : 0L;
        if (subtotalPaise < minBooking) {
            return CouponValidationResult.invalid(code, "Minimum booking of ₹" + (minBooking / 100) + " required for this coupon");
        }
        if (userId != null && c.getPerUserLimit() != null
                && redemptionRepo.countByCouponIdAndUserId(c.getId(), userId) >= c.getPerUserLimit()) {
            return CouponValidationResult.invalid(code, "You've already used this coupon");
        }
        long discount = computeDiscount(c, subtotalPaise);
        if (discount <= 0) return CouponValidationResult.invalid(code, "This coupon gives no discount on this booking");
        return new CouponValidationResult(true, c.getCode(), "Coupon applied", discount, c.getDiscountType());
    }

    private long computeDiscount(Coupon c, long subtotalPaise) {
        long discount;
        if ("PERCENT".equals(c.getDiscountType())) {
            int pct = c.getPercentOff() != null ? c.getPercentOff() : 0;
            discount = Math.round(subtotalPaise * pct / 100.0);
            if (c.getMaxDiscountPaise() != null && c.getMaxDiscountPaise() > 0) {
                discount = Math.min(discount, c.getMaxDiscountPaise());
            }
        } else { // FLAT
            discount = c.getFlatOffPaise() != null ? c.getFlatOffPaise() : 0L;
        }
        return Math.max(0L, Math.min(discount, subtotalPaise));   // never exceed the cart
    }

    /** Book a redemption on payment confirmation. Idempotent per booking. */
    @Transactional
    public void redeem(String code, UUID userId, UUID bookingId, long discountPaise) {
        if (code == null || code.isBlank() || discountPaise <= 0 || bookingId == null) return;
        if (redemptionRepo.existsByBookingId(bookingId)) return;     // already redeemed for this booking
        Coupon c = couponRepo.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (c == null) return;
        c.setUsedCount((c.getUsedCount() != null ? c.getUsedCount() : 0) + 1);
        couponRepo.save(c);
        redemptionRepo.save(CouponRedemption.builder()
                .couponId(c.getId()).userId(userId).bookingId(bookingId).discountPaise(discountPaise).build());
    }

    // ── CRUD ───────────────────────────────────────────────────────────────

    @Transactional
    public Coupon create(CreateCouponRequest req, UUID createdBy, boolean asHost) {
        String code = req.code() != null ? req.code().trim().toUpperCase() : null;
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon code required");
        if (couponRepo.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Coupon code already exists: " + code);
        if (!"PERCENT".equals(req.discountType()) && !"FLAT".equals(req.discountType())) {
            throw new IllegalArgumentException("discountType must be PERCENT or FLAT");
        }
        return couponRepo.save(Coupon.builder()
                .code(code)
                .scope(asHost ? "HOST" : (req.scope() != null ? req.scope() : "PLATFORM"))
                .ownerId(asHost ? createdBy : null)
                .listingId(req.listingId())
                .discountType(req.discountType())
                .percentOff(req.percentOff())
                .maxDiscountPaise(req.maxDiscountPaise())
                .flatOffPaise(req.flatOffPaise())
                .minBookingPaise(req.minBookingPaise() != null ? req.minBookingPaise() : 0L)
                .validFrom(req.validFrom())
                .validUntil(req.validUntil())
                .usageLimit(req.usageLimit())
                .perUserLimit(req.perUserLimit())
                .active(req.active() == null || req.active())
                .description(req.description())
                .createdBy(createdBy)
                .build());
    }

    public List<Coupon> listAll() { return couponRepo.findByScopeOrderByCreatedAtDesc("PLATFORM"); }

    public List<Coupon> listForOwner(UUID ownerId) { return couponRepo.findByOwnerIdOrderByCreatedAtDesc(ownerId); }

    @Transactional
    public Coupon setActive(UUID id, boolean active, UUID callerId, boolean isAdmin) {
        Coupon c = couponRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        if (!isAdmin && (c.getOwnerId() == null || !c.getOwnerId().equals(callerId))) {
            throw new AccessDeniedException("Not your coupon");
        }
        c.setActive(active);
        return couponRepo.save(c);
    }
}
