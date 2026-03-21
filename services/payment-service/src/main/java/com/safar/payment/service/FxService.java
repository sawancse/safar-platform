package com.safar.payment.service;

import com.safar.payment.entity.FxLock;
import com.safar.payment.entity.FxRate;
import com.safar.payment.repository.FxLockRepository;
import com.safar.payment.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FxService {

    private final FxRateRepository fxRateRepository;
    private final FxLockRepository fxLockRepository;

    // Hardcoded rates for MVP — Phase 2: fetch from Open Exchange Rates API
    private static final Map<String, BigDecimal> RATES_TO_INR = Map.of(
            "USD", new BigDecimal("83.50"),
            "EUR", new BigDecimal("91.20"),
            "GBP", new BigDecimal("106.30"),
            "JPY", new BigDecimal("0.56"),
            "AED", new BigDecimal("22.73"),
            "SGD", new BigDecimal("62.40"),
            "THB", new BigDecimal("2.35"),
            "INR", BigDecimal.ONE
    );

    private static final BigDecimal FX_MARGIN = new BigDecimal("0.025"); // 2.5% margin

    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) return BigDecimal.ONE;
        BigDecimal fromToInr = RATES_TO_INR.getOrDefault(fromCurrency, BigDecimal.ONE);
        BigDecimal toToInr = RATES_TO_INR.getOrDefault(toCurrency, BigDecimal.ONE);
        BigDecimal rate = fromToInr.divide(toToInr, 6, RoundingMode.HALF_UP);
        // Apply margin (guest pays slightly more)
        BigDecimal marginRate = rate.multiply(BigDecimal.ONE.add(FX_MARGIN));
        return marginRate.setScale(6, RoundingMode.HALF_UP);
    }

    @Transactional
    public FxLock lockRate(String sourceCurrency, long sourceAmount, UUID bookingId) {
        BigDecimal rate = getRate(sourceCurrency, "INR");
        long targetPaise = BigDecimal.valueOf(sourceAmount).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue();

        FxLock lock = new FxLock();
        lock.setSourceCurrency(sourceCurrency);
        lock.setTargetCurrency("INR");
        lock.setLockedRate(rate);
        lock.setSourceAmount(sourceAmount);
        lock.setTargetAmountPaise(targetPaise);
        lock.setBookingId(bookingId);
        lock.setLockedAt(OffsetDateTime.now());
        lock.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        lock.setUsed(false);

        return fxLockRepository.save(lock);
    }

    @Transactional
    public FxLock useLock(UUID lockId) {
        FxLock lock = fxLockRepository.findByIdAndUsedFalse(lockId)
                .orElseThrow(() -> new IllegalStateException("FX lock not found or already used"));
        if (lock.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("FX lock expired");
        }
        lock.setUsed(true);
        return fxLockRepository.save(lock);
    }

    public Optional<FxLock> getLockForBooking(UUID bookingId) {
        return fxLockRepository.findByBookingId(bookingId);
    }

    public Set<String> supportedCurrencies() {
        return RATES_TO_INR.keySet();
    }
}
