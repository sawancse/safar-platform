package com.safar.listing.service;

import com.safar.listing.dto.AvailabilityDayDto;
import com.safar.listing.dto.AvailabilityRequest;
import com.safar.listing.dto.AvailabilityResponse;
import com.safar.listing.dto.BulkAvailabilityRequest;
import com.safar.listing.entity.Availability;
import com.safar.listing.repository.AvailabilityRepository;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final ListingRepository listingRepository;

    public List<AvailabilityResponse> getAvailability(UUID listingId, LocalDate from, LocalDate to) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }
        return availabilityRepository
                .findByListingIdAndDateBetween(listingId, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AvailabilityResponse upsertAvailability(UUID listingId, AvailabilityRequest req) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }
        Availability av = availabilityRepository
                .findByListingIdAndDate(listingId, req.date())
                .orElse(Availability.builder().listingId(listingId).date(req.date()).build());

        av.setIsAvailable(req.isAvailable());
        av.setPriceOverridePaise(req.priceOverridePaise());
        av.setMinStayNights(req.minStayNights() != null ? req.minStayNights() : 1);

        return toResponse(availabilityRepository.save(av));
    }

    @Transactional
    public List<AvailabilityResponse> bulkUpsertAvailability(UUID listingId, BulkAvailabilityRequest req) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }
        if (req.fromDate().isAfter(req.toDate())) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }

        List<AvailabilityResponse> results = new ArrayList<>();
        LocalDate current = req.fromDate();
        while (!current.isAfter(req.toDate())) {
            Availability av = availabilityRepository
                    .findByListingIdAndDate(listingId, current)
                    .orElse(Availability.builder().listingId(listingId).date(current).build());

            av.setIsAvailable(req.isAvailable());
            if (req.priceOverridePaise() != null) {
                av.setPriceOverridePaise(req.priceOverridePaise());
            }
            av.setMinStayNights(req.minStayNights() != null ? req.minStayNights() : av.getMinStayNights());
            av.setMaxStayNights(req.maxStayNights() != null ? req.maxStayNights() : av.getMaxStayNights());

            results.add(toResponse(availabilityRepository.save(av)));
            current = current.plusDays(1);
        }
        return results;
    }

    @Transactional
    public List<AvailabilityResponse> bulkUpsertAvailabilityWithSource(UUID listingId, BulkAvailabilityRequest req, String source) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }
        if (req.fromDate().isAfter(req.toDate())) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }

        List<AvailabilityResponse> results = new ArrayList<>();
        LocalDate current = req.fromDate();
        while (!current.isAfter(req.toDate())) {
            Availability av = availabilityRepository
                    .findByListingIdAndDate(listingId, current)
                    .orElse(Availability.builder().listingId(listingId).date(current).build());

            av.setIsAvailable(req.isAvailable());
            av.setSource(source);
            if (req.priceOverridePaise() != null) {
                av.setPriceOverridePaise(req.priceOverridePaise());
            }
            av.setMinStayNights(req.minStayNights() != null ? req.minStayNights() : av.getMinStayNights());
            av.setMaxStayNights(req.maxStayNights() != null ? req.maxStayNights() : av.getMaxStayNights());

            results.add(toResponse(availabilityRepository.save(av)));
            current = current.plusDays(1);
        }
        return results;
    }

    public List<AvailabilityDayDto> getMonthAvailability(UUID listingId, int year, int month) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<Availability> records = availabilityRepository.findByListingIdAndDateBetween(listingId, from, to);
        Map<LocalDate, Availability> byDate = records.stream()
                .collect(Collectors.toMap(Availability::getDate, Function.identity()));

        List<AvailabilityDayDto> days = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            Availability av = byDate.get(current);
            if (av != null) {
                days.add(new AvailabilityDayDto(
                        current,
                        Boolean.TRUE.equals(av.getIsAvailable()),
                        av.getPriceOverridePaise(),
                        av.getMinStayNights(),
                        av.getMaxStayNights(),
                        false // hasBooking — would require cross-service call to booking-service
                ));
            } else {
                // No record means default available, no overrides
                days.add(new AvailabilityDayDto(current, true, null, null, null, false));
            }
            current = current.plusDays(1);
        }
        return days;
    }

    /**
     * Check if all dates in range are available for the listing.
     * Returns true if no blocked dates exist in the range.
     */
    public boolean areDatesAvailable(UUID listingId, LocalDate checkIn, LocalDate checkOut) {
        List<Availability> records = availabilityRepository.findByListingIdAndDateBetween(
                listingId, checkIn, checkOut.minusDays(1));
        for (Availability av : records) {
            if (!Boolean.TRUE.equals(av.getIsAvailable())) {
                return false;
            }
        }
        return true;
    }

    private AvailabilityResponse toResponse(Availability a) {
        return new AvailabilityResponse(
                a.getId(), a.getListingId(),
                a.getDate(), a.getIsAvailable(),
                a.getPriceOverridePaise(), a.getMinStayNights(),
                a.getMaxStayNights()
        );
    }
}
