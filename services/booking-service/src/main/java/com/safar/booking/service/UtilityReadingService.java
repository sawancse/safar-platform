package com.safar.booking.service;

import com.safar.booking.dto.RecordUtilityReadingRequest;
import com.safar.booking.dto.UtilityReadingResponse;
import com.safar.booking.entity.UtilityReading;
import com.safar.booking.entity.enums.UtilityType;
import com.safar.booking.repository.UtilityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilityReadingService {

    private final UtilityReadingRepository readingRepository;

    @Transactional
    public UtilityReading recordReading(UUID tenancyId, RecordUtilityReadingRequest req) {
        BigDecimal units = req.currentReading().subtract(req.previousReading());
        if (units.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Current reading must be >= previous reading");
        }

        long totalCharge = units.multiply(BigDecimal.valueOf(req.ratePerUnitPaise())).longValue();
        LocalDate date = req.readingDate() != null ? req.readingDate() : LocalDate.now();

        UtilityReading reading = UtilityReading.builder()
                .tenancyId(tenancyId)
                .utilityType(UtilityType.valueOf(req.utilityType()))
                .readingDate(date)
                .meterNumber(req.meterNumber())
                .previousReading(req.previousReading())
                .currentReading(req.currentReading())
                .unitsConsumed(units)
                .ratePerUnitPaise(req.ratePerUnitPaise())
                .totalChargePaise(totalCharge)
                .billingMonth(date.getMonthValue())
                .billingYear(date.getYear())
                .photoUrl(req.photoUrl())
                .build();

        UtilityReading saved = readingRepository.save(reading);
        log.info("Utility reading recorded: {} {} units for tenancy {}", req.utilityType(), units, tenancyId);
        return saved;
    }

    public List<UtilityReading> getReadings(UUID tenancyId, String utilityType) {
        if (utilityType != null && !utilityType.isEmpty()) {
            return readingRepository.findByTenancyIdAndUtilityTypeOrderByReadingDateDesc(
                    tenancyId, UtilityType.valueOf(utilityType));
        }
        return readingRepository.findByTenancyIdOrderByReadingDateDesc(tenancyId);
    }

    public long getUnbilledElectricity(UUID tenancyId) {
        return readingRepository.findByTenancyIdAndInvoiceIdIsNull(tenancyId).stream()
                .filter(r -> r.getUtilityType() == UtilityType.ELECTRICITY)
                .mapToLong(UtilityReading::getTotalChargePaise)
                .sum();
    }

    public long getUnbilledWater(UUID tenancyId) {
        return readingRepository.findByTenancyIdAndInvoiceIdIsNull(tenancyId).stream()
                .filter(r -> r.getUtilityType() == UtilityType.WATER)
                .mapToLong(UtilityReading::getTotalChargePaise)
                .sum();
    }

    @Transactional
    public void markBilled(UUID tenancyId, UUID invoiceId) {
        List<UtilityReading> unbilled = readingRepository.findByTenancyIdAndInvoiceIdIsNull(tenancyId);
        unbilled.forEach(r -> {
            r.setInvoiceId(invoiceId);
            readingRepository.save(r);
        });
    }

    public UtilityReadingResponse toResponse(UtilityReading r) {
        return new UtilityReadingResponse(
                r.getId(), r.getTenancyId(), r.getUtilityType().name(),
                r.getMeterNumber(), r.getReadingDate(),
                r.getPreviousReading(), r.getCurrentReading(), r.getUnitsConsumed(),
                r.getRatePerUnitPaise(), r.getTotalChargePaise(),
                r.getBillingMonth(), r.getBillingYear(),
                r.getInvoiceId(), r.getPhotoUrl()
        );
    }
}
