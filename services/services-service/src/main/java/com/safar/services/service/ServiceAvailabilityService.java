package com.safar.services.service;

import com.safar.services.dto.AvailabilityResponse;
import com.safar.services.dto.SetAvailabilityRequest;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.ServiceListingAvailability;
import com.safar.services.repository.ServiceListingAvailabilityRepository;
import com.safar.services.repository.ServiceListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Read + write of a listing's availability calendar (table from V23, previously
 * exposed only through the "available on date" search filter — no CRUD existed).
 * Vendors block / open dates; customers read open dates on the storefront.
 */
@Service
@RequiredArgsConstructor
public class ServiceAvailabilityService {

    private static final Set<String> VENDOR_SETTABLE = Set.of("AVAILABLE", "BLACKOUT", "HIGH_DEMAND");

    private final ServiceListingAvailabilityRepository availabilityRepo;
    private final ServiceListingRepository listingRepo;

    /** Public: calendar rows in a date window. */
    public List<AvailabilityResponse> list(UUID listingId, LocalDate from, LocalDate to) {
        return availabilityRepo
                .findByServiceListingIdAndDateBetweenOrderByDate(listingId, from, to)
                .stream().map(AvailabilityResponse::from).toList();
    }

    /** Vendor: upsert a status across a batch of dates (BOOKED rows are untouched). */
    @Transactional
    public List<AvailabilityResponse> set(UUID listingId, UUID vendorUserId, SetAvailabilityRequest req) {
        ServiceListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Not your listing");
        }
        if (req.dates() == null || req.dates().isEmpty()) {
            throw new IllegalArgumentException("At least one date is required");
        }
        String status = req.status() != null ? req.status().toUpperCase() : "AVAILABLE";
        if (!VENDOR_SETTABLE.contains(status)) {
            throw new IllegalArgumentException("status must be AVAILABLE, BLACKOUT or HIGH_DEMAND");
        }

        List<AvailabilityResponse> out = new ArrayList<>();
        for (LocalDate d : req.dates()) {
            ServiceListingAvailability row = availabilityRepo
                    .findByServiceListingIdAndDate(listingId, d)
                    .orElseGet(() -> ServiceListingAvailability.builder()
                            .serviceListingId(listingId)
                            .date(d)
                            .build());
            // A confirmed booking holds the date — never let a manual edit clobber it.
            if ("BOOKED".equalsIgnoreCase(row.getStatus())) {
                out.add(AvailabilityResponse.from(row));
                continue;
            }
            row.setStatus(status);
            row.setNotes(req.notes());
            out.add(AvailabilityResponse.from(availabilityRepo.save(row)));
        }
        return out;
    }
}
