package com.safar.booking.service;

import com.safar.booking.repository.PgTenancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nightly reconciliation: push the per-room-type "live tenancy bed count" to
 * listing-service.room_types.occupied_beds, healing drift from booking-create
 * (+1) + tenancy-create (+1) double-increments, cancelled-without-decrement
 * paths, and pre-fireOccupancyEvent legacy data.
 *
 * Truth source: COUNT(DISTINCT bed_number) on bookings.pg_tenancies for
 * statuses ACTIVE + NOTICE_PERIOD. Pushed via the existing
 * /api/v1/internal/room-types/{id}/set-occupancy?beds=N endpoint, which is
 * already clamp-aware and audit-logged in listing-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OccupancyReconcileJob {

    private final PgTenancyRepository pgTenancyRepo;
    private final RestTemplate restTemplate;

    @Value("${services.listing-service.url}")
    private String listingServiceUrl;

    /** 03:30 IST every night (Asia/Kolkata). */
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Kolkata")
    public void reconcileOccupancy() {
        List<Object[]> rows = pgTenancyRepo.countOccupiedBedsByRoomType();
        int pushed = 0;
        for (Object[] row : rows) {
            UUID roomTypeId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            try {
                restTemplate.postForObject(
                        listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId
                                + "/set-occupancy?beds=" + count,
                        null, Void.class);
                pushed++;
            } catch (Exception e) {
                log.warn("Reconcile push failed for room type {}: {}", roomTypeId, e.getMessage());
            }
        }
        log.info("OccupancyReconcileJob: pushed {} room-type occupancy values", pushed);
    }
}
