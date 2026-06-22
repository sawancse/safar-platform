package com.safar.booking.repository;

import com.safar.booking.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    long countByCouponIdAndUserId(UUID couponId, UUID userId);

    boolean existsByBookingId(UUID bookingId);
}
