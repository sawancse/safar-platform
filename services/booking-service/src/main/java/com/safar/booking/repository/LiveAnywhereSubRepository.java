package com.safar.booking.repository;

import com.safar.booking.entity.LiveAnywhereSubscription;
import com.safar.booking.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiveAnywhereSubRepository extends JpaRepository<LiveAnywhereSubscription, UUID> {
    Optional<LiveAnywhereSubscription> findByGuestIdAndStatus(UUID guestId, SubscriptionStatus status);
    boolean existsByGuestIdAndStatus(UUID guestId, SubscriptionStatus status);
}
