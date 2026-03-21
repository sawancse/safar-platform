package com.safar.user.service;

import com.safar.user.entity.NomadPrimeMembership;
import com.safar.user.repository.NomadPrimeMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NomadPrimeServiceTest {

    @Mock NomadPrimeMembershipRepository repo;
    @Mock KafkaTemplate<String, String> kafka;
    @InjectMocks NomadPrimeService service;

    private final UUID GUEST_ID = UUID.randomUUID();

    @Test
    void subscribe_activeMember_15pctDiscountApplied() {
        // Given: guest subscribes successfully
        when(repo.existsByGuestIdAndStatus(GUEST_ID, "ACTIVE")).thenReturn(false);
        NomadPrimeMembership saved = NomadPrimeMembership.builder()
                .id(UUID.randomUUID())
                .guestId(GUEST_ID)
                .status("ACTIVE")
                .discountPct(15)
                .nextRenewalDate(LocalDate.now().plusMonths(1))
                .build();
        when(repo.save(any())).thenReturn(saved);

        NomadPrimeMembership result = service.subscribe(GUEST_ID);

        assertThat(result.getDiscountPct()).isEqualTo(15);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        // Verify discount calculation: 10000 paise -> 8500 paise (15% off)
        when(repo.existsByGuestIdAndStatus(GUEST_ID, "ACTIVE")).thenReturn(true);
        long discounted = service.applyDiscount(GUEST_ID, 10000L);
        assertThat(discounted).isEqualTo(8500L);
    }

    @Test
    void subscribe_duplicate_rejected() {
        when(repo.existsByGuestIdAndStatus(GUEST_ID, "ACTIVE")).thenReturn(true);

        assertThatThrownBy(() -> service.subscribe(GUEST_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Active NomadPrime membership already exists");
    }

    @Test
    void applyDiscount_nonMember_returnsOriginalAmount() {
        when(repo.existsByGuestIdAndStatus(GUEST_ID, "ACTIVE")).thenReturn(false);

        long result = service.applyDiscount(GUEST_ID, 10000L);

        assertThat(result).isEqualTo(10000L);
    }
}
