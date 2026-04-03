package com.safar.listing.service;

import com.safar.listing.dto.LocalityPriceTrendResponse;
import com.safar.listing.entity.LocalityPriceTrend;
import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.enums.SalePropertyStatus;
import com.safar.listing.repository.LocalityPriceTrendRepository;
import com.safar.listing.repository.SalePropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalityPriceTrendService {

    private final LocalityPriceTrendRepository trendRepository;
    private final SalePropertyRepository salePropertyRepository;

    public List<LocalityPriceTrendResponse> getTrends(String city, String locality) {
        return trendRepository.findByCityAndLocality(city, locality)
                .stream().map(this::toResponse).toList();
    }

    public List<LocalityPriceTrendResponse> getCityLocalities(String city) {
        return trendRepository.findLatestByCityOrderByPrice(city)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Monthly aggregation job: runs on 1st of each month at 3 AM.
     * Aggregates active sale property prices by city + locality + type.
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void aggregateMonthlyTrends() {
        LocalDate month = LocalDate.now().withDayOfMonth(1);
        List<SaleProperty> active = salePropertyRepository.findByStatus(SalePropertyStatus.ACTIVE);

        Map<String, List<SaleProperty>> grouped = active.stream()
                .filter(sp -> sp.getCity() != null && sp.getPricePerSqftPaise() != null)
                .collect(Collectors.groupingBy(sp ->
                        sp.getCity().toLowerCase() + "|" +
                                (sp.getLocality() != null ? sp.getLocality().toLowerCase() : "unknown") + "|" +
                                sp.getSalePropertyType().name()
                ));

        int count = 0;
        for (var entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            List<SaleProperty> props = entry.getValue();

            List<Long> prices = props.stream()
                    .map(SaleProperty::getPricePerSqftPaise)
                    .sorted().toList();

            long avg = prices.stream().mapToLong(Long::longValue).sum() / prices.size();
            long median = prices.get(prices.size() / 2);
            long sold = props.stream().filter(sp -> sp.getStatus() == SalePropertyStatus.SOLD).count();

            LocalityPriceTrend trend = LocalityPriceTrend.builder()
                    .city(parts[0])
                    .locality(parts[1])
                    .month(month)
                    .avgPricePerSqftPaise(avg)
                    .medianPricePerSqftPaise(median)
                    .totalListings(props.size())
                    .totalSold((int) sold)
                    .propertyType(parts[2])
                    .build();

            trendRepository.save(trend);
            count++;
        }
        log.info("Aggregated {} locality price trends for {}", count, month);
    }

    private LocalityPriceTrendResponse toResponse(LocalityPriceTrend t) {
        return new LocalityPriceTrendResponse(
                t.getCity(), t.getLocality(), t.getMonth(),
                t.getAvgPricePerSqftPaise(), t.getMedianPricePerSqftPaise(),
                t.getTotalListings(), t.getTotalSold(), t.getPropertyType()
        );
    }
}
