package com.safar.payment.repository;

import com.safar.payment.entity.TenancySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenancySubscriptionRepository extends JpaRepository<TenancySubscription, UUID> {

    Optional<TenancySubscription> findByTenancyId(UUID tenancyId);

    Optional<TenancySubscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
