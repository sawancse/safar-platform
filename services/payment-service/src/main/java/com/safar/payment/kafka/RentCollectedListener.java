package com.safar.payment.kafka;

import com.safar.payment.entity.HostPayout;
import com.safar.payment.service.HostPayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentCollectedListener {

    private final HostPayoutService hostPayoutService;

    @KafkaListener(topics = "tenancy.rent.collected", groupId = "payment-service")
    public void onRentCollected(Map<String, Object> event) {
        UUID tenancyId = UUID.fromString((String) event.get("tenancyId"));
        UUID invoiceId = UUID.fromString((String) event.get("invoiceId"));
        long amountPaise = ((Number) event.get("amountPaise")).longValue();
        UUID hostId = UUID.fromString((String) event.get("hostId"));
        int commissionBps = ((Number) event.get("commissionBps")).intValue();

        log.info("Rent collected for tenancy {}: {} paise, processing host payout (commission: {}bps)",
                tenancyId, amountPaise, commissionBps);

        HostPayout payout = hostPayoutService.processRentPayout(
                tenancyId, invoiceId, amountPaise, hostId, commissionBps);

        if (payout != null) {
            // Auto-execute transfer immediately
            hostPayoutService.executeTransfer(payout.getId());
        }
    }
}
