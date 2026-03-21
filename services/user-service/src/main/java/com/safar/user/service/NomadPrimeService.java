package com.safar.user.service;

import com.safar.user.entity.NomadPrimeMembership;
import com.safar.user.repository.NomadPrimeMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NomadPrimeService {

    public static final long NOMAD_PRIME_PRICE_PAISE = 99_900L;

    private final NomadPrimeMembershipRepository repo;
    private final KafkaTemplate<String, String> kafka;

    @Transactional
    public NomadPrimeMembership subscribe(UUID guestId) {
        if (repo.existsByGuestIdAndStatus(guestId, "ACTIVE")) {
            throw new IllegalStateException("Active NomadPrime membership already exists");
        }
        NomadPrimeMembership membership = NomadPrimeMembership.builder()
                .guestId(guestId)
                .status("ACTIVE")
                .nextRenewalDate(LocalDate.now().plusMonths(1))
                .build();
        NomadPrimeMembership saved = repo.save(membership);
        kafka.send("nomad_prime.subscribed", guestId.toString(), guestId.toString());
        return saved;
    }

    @Transactional
    public NomadPrimeMembership cancel(UUID guestId) {
        NomadPrimeMembership membership = repo.findByGuestIdAndStatus(guestId, "ACTIVE")
                .orElseThrow(() -> new NoSuchElementException("No active NomadPrime membership found"));
        membership.setStatus("CANCELLED");
        membership.setCancelledAt(OffsetDateTime.now());
        return repo.save(membership);
    }

    public long applyDiscount(UUID guestId, long amountPaise) {
        if (repo.existsByGuestIdAndStatus(guestId, "ACTIVE")) {
            return amountPaise * 85 / 100;
        }
        return amountPaise;
    }

    public NomadPrimeMembership getMembership(UUID guestId) {
        return repo.findByGuestIdAndStatus(guestId, "ACTIVE")
                .orElseThrow(() -> new NoSuchElementException("No active NomadPrime membership found"));
    }
}
