package com.safar.booking.service;

import com.safar.booking.entity.MilesBalance;
import com.safar.booking.entity.MilesLedger;
import com.safar.booking.entity.enums.MilesTier;
import com.safar.booking.repository.MilesBalanceRepository;
import com.safar.booking.repository.MilesLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilesService {

    private final MilesBalanceRepository balanceRepo;
    private final MilesLedgerRepository ledgerRepo;
    private final KafkaTemplate<String, String> kafka;

    // 10 miles = 1 INR = 100 paise
    private static final long PAISE_PER_MILE = 10L;

    /**
     * Earn miles for a completed booking.
     * Rate per 100 paise (1 INR): BRONZE=1, SILVER=1.5, GOLD=2, PLATINUM=3
     */
    @Transactional
    public MilesBalance earnMiles(UUID userId, UUID bookingId, long amountPaise) {
        MilesBalance balance = getOrCreateBalance(userId);
        double multiplier = getEarnMultiplier(balance.getTier());
        long earned = Math.round((amountPaise / 100.0) * multiplier);

        balance.setBalance(balance.getBalance() + earned);
        balance.setLifetime(balance.getLifetime() + earned);
        balance.setTier(computeTier(balance.getLifetime()));

        MilesBalance saved = balanceRepo.save(balance);

        ledgerRepo.save(MilesLedger.builder()
                .userId(userId)
                .bookingId(bookingId)
                .transactionType("EARN")
                .amount(earned)
                .balanceAfter(saved.getBalance())
                .description("Earned " + earned + " miles for booking")
                .build());

        kafka.send("miles.earned", userId.toString());
        log.info("User {} earned {} miles for booking {}", userId, earned, bookingId);
        return saved;
    }

    /**
     * Redeem miles against a booking.
     * 10 miles = 1 INR (100 paise).
     * Discount cap by tier: BRONZE 10%, SILVER 15%, GOLD 20%, PLATINUM 25%.
     */
    @Transactional
    public long redeemMiles(UUID userId, UUID bookingId, long requestedMiles, long totalPaise) {
        MilesBalance balance = balanceRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No miles balance found for user: " + userId));

        if (balance.getBalance() < requestedMiles) {
            throw new IllegalArgumentException("Insufficient miles. Available: " + balance.getBalance()
                    + ", requested: " + requestedMiles);
        }

        // Cap discount by tier percentage
        double maxDiscountPercent = getMaxDiscountPercent(balance.getTier());
        long maxDiscountPaise = Math.round(totalPaise * maxDiscountPercent);
        long requestedDiscountPaise = requestedMiles * PAISE_PER_MILE;
        long actualDiscountPaise = Math.min(requestedDiscountPaise, maxDiscountPaise);
        long actualMilesUsed = actualDiscountPaise / PAISE_PER_MILE;

        balance.setBalance(balance.getBalance() - actualMilesUsed);
        balanceRepo.save(balance);

        ledgerRepo.save(MilesLedger.builder()
                .userId(userId)
                .bookingId(bookingId)
                .transactionType("REDEEM")
                .amount(-actualMilesUsed)
                .balanceAfter(balance.getBalance())
                .description("Redeemed " + actualMilesUsed + " miles for booking discount")
                .build());

        kafka.send("miles.redeemed", userId.toString());
        log.info("User {} redeemed {} miles for booking {}", userId, actualMilesUsed, bookingId);
        return actualDiscountPaise;
    }

    public MilesBalance getBalance(UUID userId) {
        return getOrCreateBalance(userId);
    }

    public Page<MilesLedger> getHistory(UUID userId, Pageable pageable) {
        return ledgerRepo.findByUserId(userId, pageable);
    }

    public MilesTier computeTier(long lifetime) {
        if (lifetime >= 200_000) return MilesTier.PLATINUM;
        if (lifetime >= 50_000) return MilesTier.GOLD;
        if (lifetime >= 10_000) return MilesTier.SILVER;
        return MilesTier.BRONZE;
    }

    private MilesBalance getOrCreateBalance(UUID userId) {
        return balanceRepo.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return balanceRepo.save(MilesBalance.builder()
                                .userId(userId)
                                .balance(0L)
                                .lifetime(0L)
                                .tier(MilesTier.BRONZE)
                                .build());
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Race condition: another request created it first — just fetch it
                        return balanceRepo.findByUserId(userId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private double getEarnMultiplier(MilesTier tier) {
        return switch (tier) {
            case BRONZE -> 1.0;
            case SILVER -> 1.5;
            case GOLD -> 2.0;
            case PLATINUM -> 3.0;
        };
    }

    private double getMaxDiscountPercent(MilesTier tier) {
        return switch (tier) {
            case BRONZE -> 0.10;
            case SILVER -> 0.15;
            case GOLD -> 0.20;
            case PLATINUM -> 0.25;
        };
    }
}
