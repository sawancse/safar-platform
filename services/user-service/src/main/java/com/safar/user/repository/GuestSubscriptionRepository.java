package com.safar.user.repository;

import com.safar.user.entity.GuestSubscription;
import com.safar.user.entity.enums.GuestSubStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestSubscriptionRepository extends JpaRepository<GuestSubscription, UUID> {
    Optional<GuestSubscription> findByGuestId(UUID guestId);
    boolean existsByGuestId(UUID guestId);
    List<GuestSubscription> findAllByStatus(GuestSubStatus status);
}
