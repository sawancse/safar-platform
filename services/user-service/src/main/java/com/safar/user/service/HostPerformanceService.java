package com.safar.user.service;

import com.safar.user.entity.HostSubscription;
import com.safar.user.entity.enums.KycStatus;
import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.repository.HostKycRepository;
import com.safar.user.repository.HostSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostPerformanceService {

    private final HostSubscriptionRepository subscriptionRepository;
    private final HostKycRepository hostKycRepository;

    /**
     * Recalculate performance metrics for a single host.
     * Current simplified logic:
     * - If KYC is VERIFIED: commissionDiscountPercent = 2, preferredPartner = true
     * - Otherwise: commissionDiscountPercent = 0, preferredPartner = false
     *
     * Future: query booking-service for occupancy rate, review-service for avg rating,
     * and compute tiered discounts.
     */
    @Transactional
    public void recalculatePerformance(UUID hostId) {
        HostSubscription sub = subscriptionRepository.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("No subscription for host " + hostId));

        boolean kycVerified = hostKycRepository.findByUserId(hostId)
                .map(kyc -> kyc.getStatus() == KycStatus.VERIFIED)
                .orElse(false);

        if (kycVerified) {
            sub.setCommissionDiscountPercent(2);
            sub.setPreferredPartner(true);
        } else {
            sub.setCommissionDiscountPercent(0);
            sub.setPreferredPartner(false);
        }

        sub.setPerformanceUpdatedAt(OffsetDateTime.now());
        subscriptionRepository.save(sub);
        log.info("Performance recalculated for host {}: discount={}%, preferred={}",
                hostId, sub.getCommissionDiscountPercent(), sub.getPreferredPartner());
    }

    /**
     * Recalculate performance for all active/trial subscriptions.
     */
    @Transactional
    public int recalculateAll() {
        List<HostSubscription> activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.TRIAL)
                .toList();

        int updated = 0;
        for (HostSubscription sub : activeSubscriptions) {
            try {
                recalculatePerformance(sub.getHostId());
                updated++;
            } catch (Exception e) {
                log.warn("Failed to recalculate performance for host {}: {}",
                        sub.getHostId(), e.getMessage());
            }
        }
        log.info("Recalculated performance for {}/{} hosts", updated, activeSubscriptions.size());
        return updated;
    }
}
