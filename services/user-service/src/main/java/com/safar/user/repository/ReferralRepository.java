package com.safar.user.repository;

import com.safar.user.entity.Referral;
import com.safar.user.entity.enums.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByReferralCode(String referralCode);

    List<Referral> findByReferrerId(UUID referrerId);

    List<Referral> findByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    Optional<Referral> findByReferredId(UUID referredId);

    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);
}
