package com.safar.booking.service;

import com.safar.booking.entity.TripLeg;
import com.safar.booking.entity.enums.LegType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Routes Trip-level cancel requests to the originating service per leg.
 *
 * Universal Trip lives in booking-service but the actual booking rows
 * live across multiple services (flight-service, insurance-service,
 * chef-service, etc.). When a user cancels a Trip, this router fans
 * out the cancel to each leg's home service via REST.
 *
 * Best-effort: a single leg's cancel failure is logged but doesn't block
 * the other legs. The Trip-side cancel always proceeds (per Tree-3 of
 * the design — never block the user's local cancel on a downstream
 * outage; reconcile drift via the daily recon job).
 */
@Service
@Slf4j
public class LegCancelRouter {

    private final RestClient restClient = RestClient.create();

    @Value("${services.flight-service.url:http://localhost:8094}")
    private String flightServiceUrl;

    @Value("${services.insurance-service.url:http://localhost:8097}")
    private String insuranceServiceUrl;

    /**
     * Best-effort cancel of a leg's underlying booking. Returns true if
     * the downstream call succeeded; false on any failure (caller logs).
     */
    public boolean cancelLeg(TripLeg leg, String reason) {
        if (leg == null || leg.getExternalBookingId() == null || leg.getExternalService() == null) {
            return false;
        }
        try {
            return switch (leg.getLegType()) {
                case FLIGHT -> cancelFlight(leg, reason);
                case INSURANCE -> cancelInsurance(leg, reason);
                case STAY, CAB, COOK, PANDIT, DECOR, SPA, EXPERIENCE -> {
                    // TODO(cancel-router): wire stay → booking-service internal cancel,
                    // cook → services-service cancel, etc. Day-1 these legs aren't
                    // attached automatically yet; they'll be wired as each vertical's
                    // attach-to-trip flow ships.
                    log.info("Skipping downstream cancel for {} leg (not wired yet)", leg.getLegType());
                    yield true;
                }
            };
        } catch (Exception e) {
            log.warn("Cancel of {} leg {} (external {}) failed: {}",
                    leg.getLegType(), leg.getId(), leg.getExternalBookingId(), e.getMessage());
            return false;
        }
    }

    private boolean cancelFlight(TripLeg leg, String reason) {
        // Note: flight-service cancel endpoint requires the booking owner's JWT.
        // Cross-service calls don't have the user JWT — for now we use the gateway's
        // internal X-User-Id header pattern. Production: add a dedicated internal
        // cancel endpoint that trusts X-Internal-Service header + user_id query param.
        try {
            restClient.post()
                    .uri(flightServiceUrl + "/api/v1/flights/" + leg.getExternalBookingId() + "/cancel")
                    .header("X-User-Id", leg.getTrip().getUserId().toString())
                    .header("X-User-Role", "USER")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Flight leg cancel propagated: bookingId={}", leg.getExternalBookingId());
            return true;
        } catch (Exception e) {
            log.warn("Flight cancel propagation failed for {}: {}", leg.getExternalBookingId(), e.getMessage());
            return false;
        }
    }

    private boolean cancelInsurance(TripLeg leg, String reason) {
        try {
            String url = insuranceServiceUrl + "/api/v1/insurance/" + leg.getExternalBookingId() + "/cancel";
            if (reason != null && !reason.isBlank()) {
                url += "?reason=" + java.net.URLEncoder.encode(reason, java.nio.charset.StandardCharsets.UTF_8);
            }
            restClient.post()
                    .uri(url)
                    .header("X-User-Id", leg.getTrip().getUserId().toString())
                    .header("X-User-Role", "USER")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Insurance leg cancel propagated: policyId={}", leg.getExternalBookingId());
            return true;
        } catch (Exception e) {
            log.warn("Insurance cancel propagation failed for {}: {}", leg.getExternalBookingId(), e.getMessage());
            return false;
        }
    }
}
