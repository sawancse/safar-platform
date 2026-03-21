package com.safar.user.service;

import com.safar.user.dto.RazorpaySubscription;
import com.safar.user.entity.HostSubscription;
import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;
import com.safar.user.repository.HostSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HostSubscriptionServiceTest {

    @Mock HostSubscriptionRepository subscriptionRepository;
    @Mock RazorpayGateway razorpayGateway;
    @InjectMocks HostSubscriptionService service;

    private final UUID hostId = UUID.randomUUID();

    @Test
    void startTrial_new_creates90DayTrial() {
        when(subscriptionRepository.findByHostId(hostId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.startTrial(hostId, SubscriptionTier.STARTER);

        assertThat(dto.status()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(dto.tier()).isEqualTo(SubscriptionTier.STARTER);
        assertThat(dto.trialEndsAt()).isAfter(java.time.OffsetDateTime.now().plusDays(89));
    }

    @Test
    void startTrial_existing_throwsConflict() {
        when(subscriptionRepository.findByHostId(hostId))
                .thenReturn(Optional.of(HostSubscription.builder().build()));
        assertThatThrownBy(() -> service.startTrial(hostId, SubscriptionTier.STARTER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activate_callsRazorpayAndSaves() {
        HostSubscription existing = HostSubscription.builder()
                .hostId(hostId).tier(SubscriptionTier.STARTER)
                .status(SubscriptionStatus.TRIAL).billingCycle("MONTHLY").amountPaise(99900).build();
        when(subscriptionRepository.findByHostId(hostId)).thenReturn(Optional.of(existing));
        when(razorpayGateway.createSubscription(any(), any()))
                .thenReturn(new RazorpaySubscription("rzp_sub_123", "https://rzp.io/pay"));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.activate(hostId, SubscriptionTier.PRO);

        assertThat(response.razorpaySubId()).isEqualTo("rzp_sub_123");
        assertThat(response.paymentLink()).isEqualTo("https://rzp.io/pay");
        assertThat(response.tier()).isEqualTo(SubscriptionTier.PRO);
    }

    @Test
    void activate_alreadyActive_throwsConflict() {
        HostSubscription active = HostSubscription.builder()
                .hostId(hostId).status(SubscriptionStatus.ACTIVE).billingCycle("MONTHLY").amountPaise(99900).build();
        when(subscriptionRepository.findByHostId(hostId)).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.activate(hostId, SubscriptionTier.PRO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onSubscriptionActivated_setsStatusActive() {
        HostSubscription sub = HostSubscription.builder()
                .razorpaySubId("rzp_sub_123").status(SubscriptionStatus.TRIAL)
                .billingCycle("MONTHLY").amountPaise(99900).build();
        when(subscriptionRepository.findByRazorpaySubId("rzp_sub_123")).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onSubscriptionActivated("rzp_sub_123", new org.json.JSONObject());

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
