package com.safar.listing.repository;

import com.safar.listing.entity.B2bApiSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface B2bApiSubscriptionRepository extends JpaRepository<B2bApiSubscription, UUID> {
    Optional<B2bApiSubscription> findByApiKey(String apiKey);
}
