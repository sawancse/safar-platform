package com.safar.user.service;

import com.safar.user.dto.GuestSubscriptionDto;
import com.safar.user.entity.GuestSubscription;
import com.safar.user.entity.enums.GuestSubStatus;
import com.safar.user.repository.GuestSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestSubscriptionService {

    public static final long TRAVELER_PRO_PAISE = 29900L;

    private final GuestSubscriptionRepository repo;

    @Transactional
    public GuestSubscriptionDto subscribe(UUID guestId) {
        if (repo.existsByGuestId(guestId)) {
            throw new IllegalStateException("Already subscribed to Traveler Pro");
        }
        GuestSubscription sub = GuestSubscription.builder()
                .guestId(guestId)
                .status(GuestSubStatus.ACTIVE)
                .nextBillingAt(OffsetDateTime.now().plusMonths(1))
                .build();
        return toDto(repo.save(sub));
    }

    public GuestSubscriptionDto getSubscription(UUID guestId) {
        return repo.findByGuestId(guestId)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("No active subscription"));
    }

    @Transactional
    public GuestSubscriptionDto cancel(UUID guestId) {
        GuestSubscription sub = repo.findByGuestId(guestId)
                .orElseThrow(() -> new NoSuchElementException("No subscription found"));
        sub.setStatus(GuestSubStatus.CANCELLED);
        return toDto(repo.save(sub));
    }

    public boolean isTravelerPro(UUID guestId) {
        return repo.findByGuestId(guestId)
                .map(s -> s.getStatus() == GuestSubStatus.ACTIVE)
                .orElse(false);
    }

    private GuestSubscriptionDto toDto(GuestSubscription s) {
        return new GuestSubscriptionDto(
                s.getId(), s.getGuestId(), s.getStatus(),
                s.getTrialEndsAt(), s.getNextBillingAt(), s.getCreatedAt());
    }
}
