package com.safar.user.repository;

import com.safar.user.entity.HostSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface HostSubscriptionRepository extends JpaRepository<HostSubscription, UUID> {
    Optional<HostSubscription> findByHostId(UUID hostId);
    Optional<HostSubscription> findByRazorpaySubId(String razorpaySubId);
}
