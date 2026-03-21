package com.safar.booking.service;

import com.safar.booking.entity.MilesBalance;
import com.safar.booking.entity.MilesLedger;
import com.safar.booking.entity.enums.MilesTier;
import com.safar.booking.repository.MilesBalanceRepository;
import com.safar.booking.repository.MilesLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilesServiceTest {

    @Mock MilesBalanceRepository balanceRepo;
    @Mock MilesLedgerRepository ledgerRepo;
    @Mock KafkaTemplate<String, String> kafka;

    @InjectMocks MilesService milesService;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    /**
     * Test 1: Earn calculation — BRONZE tier earns 1 mile per 100 paise (1 INR).
     * A booking of 500,000 paise (INR 5,000) should earn 5,000 miles.
     */
    @Test
    void earnMiles_bronzeTier_calculatesCorrectly() {
        MilesBalance existing = MilesBalance.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(0L)
                .lifetime(0L)
                .tier(MilesTier.BRONZE)
                .build();

        when(balanceRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepo.save(any(MilesBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepo.save(any(MilesLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        MilesBalance result = milesService.earnMiles(userId, bookingId, 500_000L);

        // BRONZE: 1.0 multiplier => 500000 / 100 * 1.0 = 5000 miles
        assertThat(result.getBalance()).isEqualTo(5_000L);
        assertThat(result.getLifetime()).isEqualTo(5_000L);
        verify(kafka).send(eq("miles.earned"), eq(userId.toString()));
    }

    /**
     * Test 2: Tier upgrade at boundary — lifetime reaching 10,000 should upgrade to SILVER.
     */
    @Test
    void earnMiles_tierUpgradeAtBoundary_upgradesToSilver() {
        MilesBalance existing = MilesBalance.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(5_000L)
                .lifetime(9_000L)
                .tier(MilesTier.BRONZE)
                .build();

        when(balanceRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepo.save(any(MilesBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepo.save(any(MilesLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        // Earning 1000 miles (100,000 paise * 1.0 BRONZE multiplier = 1000 miles)
        // lifetime becomes 9000 + 1000 = 10000 => SILVER
        MilesBalance result = milesService.earnMiles(userId, bookingId, 100_000L);

        assertThat(result.getLifetime()).isEqualTo(10_000L);
        assertThat(result.getTier()).isEqualTo(MilesTier.SILVER);
    }

    /**
     * Test 3: Redeem cap enforcement — BRONZE tier can only get max 10% discount.
     * Booking total 1,000,000 paise => max discount = 100,000 paise.
     * 100,000 paise / 10 paise per mile = 10,000 miles max.
     * If user requests 20,000 miles, only 10,000 should be used.
     */
    @Test
    void redeemMiles_bronzeCap_enforcesMaxDiscount() {
        MilesBalance existing = MilesBalance.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(20_000L)
                .lifetime(20_000L)
                .tier(MilesTier.BRONZE)
                .build();

        when(balanceRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepo.save(any(MilesBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepo.save(any(MilesLedger.class))).thenAnswer(inv -> inv.getArgument(0));

        long discountPaise = milesService.redeemMiles(userId, bookingId, 20_000L, 1_000_000L);

        // BRONZE max = 10% of 1,000,000 = 100,000 paise
        // 20,000 miles * 10 = 200,000 paise requested, but capped at 100,000
        // Actual miles used = 100,000 / 10 = 10,000
        assertThat(discountPaise).isEqualTo(100_000L);
        assertThat(existing.getBalance()).isEqualTo(10_000L); // 20,000 - 10,000
    }

    /**
     * Test 4: Redeem with insufficient miles — should throw IllegalArgumentException.
     */
    @Test
    void redeemMiles_insufficientBalance_throws() {
        MilesBalance existing = MilesBalance.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(50L)
                .lifetime(50L)
                .tier(MilesTier.BRONZE)
                .build();

        when(balanceRepo.findByUserId(userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> milesService.redeemMiles(userId, bookingId, 100L, 500_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient miles");
    }
}
