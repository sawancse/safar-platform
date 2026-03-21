package com.safar.listing.service;

import com.safar.listing.entity.B2bApiSubscription;
import com.safar.listing.repository.B2bApiSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2bApiKeyValidator {

    private final B2bApiSubscriptionRepository subscriptionRepo;

    /**
     * Validates the API key: checks existence, active status, and call limit.
     * Throws SecurityException (401) if invalid, IllegalStateException (429) if limit exceeded.
     */
    public B2bApiSubscription validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new SecurityException("Missing API key");
        }

        B2bApiSubscription sub = subscriptionRepo.findByApiKey(apiKey)
                .orElseThrow(() -> new SecurityException("Invalid API key"));

        if (!"ACTIVE".equals(sub.getStatus())) {
            throw new SecurityException("API key is not active");
        }

        if (sub.getMonthlyCalls() >= sub.getCallLimit()) {
            throw new IllegalStateException("Monthly API call limit exceeded");
        }

        // Increment call count
        sub.setMonthlyCalls(sub.getMonthlyCalls() + 1);
        subscriptionRepo.save(sub);

        log.debug("B2B API call validated for company: {}", sub.getCompanyName());
        return sub;
    }
}
