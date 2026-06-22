package com.safar.booking.repository;

import com.safar.booking.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findByScopeOrderByCreatedAtDesc(String scope);

    List<Coupon> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
