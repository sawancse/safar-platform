package com.safar.chef.service;

import com.safar.chef.dto.CreateSubscriptionRequest;
import com.safar.chef.dto.ModifySubscriptionRequest;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.ChefSubscription;
import com.safar.chef.entity.enums.SubscriptionStatus;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.ChefSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefSubscriptionService {

    private final ChefSubscriptionRepository subscriptionRepo;
    private final ChefProfileRepository chefProfileRepo;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String generateSubscriptionRef() {
        StringBuilder sb = new StringBuilder("SS-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    @Transactional
    public ChefSubscription createSubscription(UUID customerId, CreateSubscriptionRequest req) {
        ChefProfile chef = chefProfileRepo.findById(req.chefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        if (!chef.getAvailable()) {
            throw new IllegalArgumentException("Chef is currently not available");
        }

        // Use provided rate or fall back to chef's monthly rate
        Long monthlyRatePaise = req.monthlyRatePaise() != null ? req.monthlyRatePaise() : chef.getMonthlyRatePaise();
        if (monthlyRatePaise == null) {
            throw new IllegalArgumentException("No monthly rate specified and chef has no default monthly rate");
        }

        long platformFeePaise = monthlyRatePaise * 10 / 100;
        long chefEarningsPaise = monthlyRatePaise - platformFeePaise;

        ChefSubscription subscription = ChefSubscription.builder()
                .subscriptionRef(generateSubscriptionRef())
                .chefId(chef.getId())
                .customerId(customerId)
                .chefName(chef.getName())
                .customerName(req.customerName())
                .plan(req.plan())
                .mealsPerDay(req.mealsPerDay() != null ? req.mealsPerDay() : 1)
                .mealTypes(req.mealTypes())
                .schedule(req.schedule())
                .monthlyRatePaise(monthlyRatePaise)
                .platformFeePaise(platformFeePaise)
                .chefEarningsPaise(chefEarningsPaise)
                .startDate(req.startDate())
                .endDate(req.startDate().plusDays(30))
                .nextRenewalDate(req.startDate().plusDays(30))
                .address(req.address())
                .city(req.city())
                .locality(req.locality())
                .pincode(req.pincode())
                .specialRequests(req.specialRequests())
                .dietaryPreferences(req.dietaryPreferences())
                .status(SubscriptionStatus.ACTIVE)
                .build();

        ChefSubscription saved = subscriptionRepo.save(subscription);
        log.info("Chef subscription created: {} ref={} chef={} customer={}", saved.getId(), saved.getSubscriptionRef(), chef.getId(), customerId);
        return saved;
    }

    @Transactional
    public ChefSubscription cancelSubscription(UUID userId, UUID subscriptionId, String reason) {
        ChefSubscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // Either the customer or the chef (by userId) can cancel
        ChefProfile chefProfile = chefProfileRepo.findByUserId(userId).orElse(null);
        boolean isChef = chefProfile != null && subscription.getChefId().equals(chefProfile.getId());
        boolean isCustomer = subscription.getCustomerId().equals(userId);

        if (!isChef && !isCustomer) {
            throw new IllegalArgumentException("Not authorized to cancel this subscription");
        }
        if (isChef) {
            chefProfile.ensureNotSuspended();
        }
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Subscription is already cancelled");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason(reason);
        subscription.setCancelledAt(OffsetDateTime.now());
        log.info("Chef subscription cancelled: {} by userId={}", subscriptionId, userId);
        return subscriptionRepo.save(subscription);
    }

    @Transactional
    public ChefSubscription modifySubscription(UUID customerId, UUID subscriptionId, ModifySubscriptionRequest req) {
        ChefSubscription subscription = subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!subscription.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to modify this subscription");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Subscription can only be modified while ACTIVE");
        }

        if (req.mealsPerDay() != null) subscription.setMealsPerDay(req.mealsPerDay());
        if (req.mealTypes() != null) subscription.setMealTypes(req.mealTypes());
        if (req.schedule() != null) subscription.setSchedule(req.schedule());
        if (req.address() != null) subscription.setAddress(req.address());
        if (req.city() != null) subscription.setCity(req.city());
        if (req.locality() != null) subscription.setLocality(req.locality());
        if (req.pincode() != null) subscription.setPincode(req.pincode());
        if (req.specialRequests() != null) subscription.setSpecialRequests(req.specialRequests());
        if (req.dietaryPreferences() != null) subscription.setDietaryPreferences(req.dietaryPreferences());

        subscription.setModifiedAt(OffsetDateTime.now());
        subscription.setModificationCount(subscription.getModificationCount() != null ? subscription.getModificationCount() + 1 : 1);

        ChefSubscription saved = subscriptionRepo.save(subscription);
        log.info("Chef subscription modified: {} by customer={}", subscriptionId, customerId);
        return saved;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ChefSubscription> adminListAll(org.springframework.data.domain.Pageable pageable) {
        return subscriptionRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<ChefSubscription> getMySubscriptions(UUID customerId) {
        return subscriptionRepo.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<ChefSubscription> getChefSubscriptions(UUID chefId) {
        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        return subscriptionRepo.findByChefId(chef.getId());
    }

    @Transactional(readOnly = true)
    public ChefSubscription getSubscription(UUID subscriptionId) {
        return subscriptionRepo.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
    }
}
