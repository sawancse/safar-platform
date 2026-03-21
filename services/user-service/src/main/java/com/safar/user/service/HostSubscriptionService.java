package com.safar.user.service;

import com.safar.user.dto.ActivateSubscriptionResponse;
import com.safar.user.dto.HostSubscriptionDto;
import com.safar.user.entity.HostSubscription;
import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;
import com.safar.user.repository.HostSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostSubscriptionService {

    private static final Map<SubscriptionTier, Integer> PRICES = Map.of(
            SubscriptionTier.STARTER,     99900,
            SubscriptionTier.PRO,        249900,
            SubscriptionTier.COMMERCIAL, 399900
    );

    private final HostSubscriptionRepository subscriptionRepository;
    private final RazorpayGateway razorpayGateway;

    public HostSubscriptionDto getSubscription(UUID hostId) {
        HostSubscription sub = subscriptionRepository.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("No subscription for host " + hostId));
        return toDto(sub);
    }

    @Transactional
    public HostSubscriptionDto startTrial(UUID hostId, SubscriptionTier tier) {
        if (subscriptionRepository.findByHostId(hostId).isPresent()) {
            throw new IllegalStateException("Subscription already exists for host " + hostId);
        }
        HostSubscription sub = HostSubscription.builder()
                .hostId(hostId)
                .tier(tier)
                .status(SubscriptionStatus.TRIAL)
                .trialEndsAt(OffsetDateTime.now().plusDays(90))
                .billingCycle("MONTHLY")
                .amountPaise(PRICES.get(tier))
                .build();
        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional
    public HostSubscriptionDto upgradeTier(UUID hostId, SubscriptionTier tier) {
        HostSubscription sub = subscriptionRepository.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("No subscription for host " + hostId));
        sub.setTier(tier);
        sub.setAmountPaise(PRICES.get(tier));
        log.info("Host {} upgraded to {} tier", hostId, tier);
        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional
    public ActivateSubscriptionResponse activate(UUID hostId, SubscriptionTier tier) {
        HostSubscription sub = subscriptionRepository.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("No subscription for host " + hostId));

        if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Subscription is already active");
        }

        var rzpSub = razorpayGateway.createSubscription(hostId, tier);

        sub.setTier(tier);
        sub.setRazorpaySubId(rzpSub.id());
        sub.setAmountPaise(PRICES.get(tier));
        subscriptionRepository.save(sub);

        return new ActivateSubscriptionResponse(rzpSub.id(), rzpSub.paymentLink(), tier, sub.getStatus());
    }

    @Transactional
    public void onSubscriptionActivated(String razorpaySubId, JSONObject entity) {
        subscriptionRepository.findByRazorpaySubId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            long chargeAt = entity.optLong("charge_at", 0);
            if (chargeAt > 0) {
                sub.setNextBillingAt(OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(chargeAt), ZoneOffset.UTC));
            }
            subscriptionRepository.save(sub);
            log.info("Subscription {} activated for host {}", razorpaySubId, sub.getHostId());
        });
    }

    @Transactional
    public void onSubscriptionCharged(String razorpaySubId, JSONObject entity) {
        subscriptionRepository.findByRazorpaySubId(razorpaySubId).ifPresent(sub -> {
            long chargeAt = entity.optLong("charge_at", 0);
            if (chargeAt > 0) {
                sub.setNextBillingAt(OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(chargeAt), ZoneOffset.UTC));
            }
            subscriptionRepository.save(sub);
        });
    }

    @Transactional
    public void onSubscriptionCancelled(String razorpaySubId) {
        subscriptionRepository.findByRazorpaySubId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(sub);
            log.info("Subscription {} cancelled for host {}", razorpaySubId, sub.getHostId());
        });
    }

    @Transactional
    public void onSubscriptionPending(String razorpaySubId) {
        subscriptionRepository.findByRazorpaySubId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.PAUSED);
            subscriptionRepository.save(sub);
            log.warn("Subscription {} payment pending for host {}", razorpaySubId, sub.getHostId());
        });
    }

    private HostSubscriptionDto toDto(HostSubscription s) {
        return new HostSubscriptionDto(
                s.getId(), s.getHostId(),
                s.getTier(), s.getStatus(),
                s.getTrialEndsAt(), s.getBillingCycle(),
                s.getAmountPaise(), s.getNextBillingAt(),
                s.getCreatedAt(),
                s.getCommissionDiscountPercent(),
                s.getPreferredPartner()
        );
    }
}
