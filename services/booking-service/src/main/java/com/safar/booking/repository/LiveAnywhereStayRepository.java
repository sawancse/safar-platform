package com.safar.booking.repository;

import com.safar.booking.entity.LiveAnywhereStay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LiveAnywhereStayRepository extends JpaRepository<LiveAnywhereStay, UUID> {
    List<LiveAnywhereStay> findBySubscriptionId(UUID subscriptionId);
}
