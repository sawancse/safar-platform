package com.safar.chef.repository;

import com.safar.chef.entity.ChefSubscription;
import com.safar.chef.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChefSubscriptionRepository extends JpaRepository<ChefSubscription, UUID> {

    List<ChefSubscription> findByCustomerId(UUID customerId);

    List<ChefSubscription> findByChefId(UUID chefId);

    List<ChefSubscription> findByStatus(SubscriptionStatus status);

    List<ChefSubscription> findByChefIdAndStatus(UUID chefId, SubscriptionStatus status);
}
