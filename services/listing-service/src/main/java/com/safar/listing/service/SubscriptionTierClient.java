package com.safar.listing.service;

import com.safar.listing.dto.SubscriptionInfoDto;
import com.safar.listing.entity.enums.HostTier;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@Slf4j
public class SubscriptionTierClient {

    private final RestClient restClient;

    public SubscriptionTierClient(
            @Value("${services.user-service.url:http://localhost:8082}") String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getTierFallback")
    @Retry(name = "userService")
    public HostTier getTier(UUID hostId) {
        SubscriptionInfoDto info = restClient.get()
                .uri("/api/v1/internal/hosts/{hostId}/subscription", hostId)
                .retrieve()
                .body(SubscriptionInfoDto.class);
        return info != null ? HostTier.from(info.tier()) : HostTier.STARTER;
    }

    public HostTier getTierFallback(UUID hostId, Throwable t) {
        log.warn("Could not reach user-service for host {} tier check, defaulting to STARTER: {}", hostId, t.getMessage());
        return HostTier.STARTER;
    }
}
