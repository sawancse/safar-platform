package com.safar.user.service;

import com.safar.user.entity.GuestSubscription;
import com.safar.user.entity.enums.GuestSubStatus;
import com.safar.user.repository.GuestSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestSubscriptionServiceTest {

    @Mock GuestSubscriptionRepository repo;
    @InjectMocks GuestSubscriptionService service;

    private final UUID GUEST_ID = UUID.randomUUID();

    @Test
    void subscribe_newGuest_succeeds() {
        when(repo.existsByGuestId(GUEST_ID)).thenReturn(false);
        GuestSubscription saved = GuestSubscription.builder()
                .id(UUID.randomUUID()).guestId(GUEST_ID)
                .status(GuestSubStatus.ACTIVE)
                .nextBillingAt(OffsetDateTime.now().plusMonths(1))
                .build();
        when(repo.save(any())).thenReturn(saved);

        var dto = service.subscribe(GUEST_ID);

        assertThat(dto.guestId()).isEqualTo(GUEST_ID);
        assertThat(dto.status()).isEqualTo(GuestSubStatus.ACTIVE);
    }

    @Test
    void subscribe_duplicate_throws409() {
        when(repo.existsByGuestId(GUEST_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.subscribe(GUEST_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Already subscribed");
    }

    @Test
    void cancel_existing_setsStatusCancelled() {
        GuestSubscription sub = GuestSubscription.builder()
                .id(UUID.randomUUID()).guestId(GUEST_ID)
                .status(GuestSubStatus.ACTIVE).build();
        when(repo.findByGuestId(GUEST_ID)).thenReturn(Optional.of(sub));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.cancel(GUEST_ID);

        assertThat(dto.status()).isEqualTo(GuestSubStatus.CANCELLED);
    }

    @Test
    void cancel_noSub_throwsNotFound() {
        when(repo.findByGuestId(GUEST_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(GUEST_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void isTravelerPro_activeSub_returnsTrue() {
        GuestSubscription sub = GuestSubscription.builder()
                .guestId(GUEST_ID).status(GuestSubStatus.ACTIVE).build();
        when(repo.findByGuestId(GUEST_ID)).thenReturn(Optional.of(sub));
        assertThat(service.isTravelerPro(GUEST_ID)).isTrue();
    }

    @Test
    void isTravelerPro_noSub_returnsFalse() {
        when(repo.findByGuestId(GUEST_ID)).thenReturn(Optional.empty());
        assertThat(service.isTravelerPro(GUEST_ID)).isFalse();
    }
}
