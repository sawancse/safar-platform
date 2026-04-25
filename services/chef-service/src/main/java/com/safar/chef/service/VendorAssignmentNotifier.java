package com.safar.chef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.chef.entity.EventBooking;
import com.safar.chef.entity.EventBookingVendor;
import com.safar.chef.entity.PartnerVendor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits vendor.assigned events to Kafka so notification-service can dispatch
 * email + WhatsApp template to the vendor (and a confirmation email to the
 * customer). Uses the resilient outbox pattern — guaranteed delivery even if
 * Kafka is briefly down.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VendorAssignmentNotifier {

    private static final String TOPIC_VENDOR_ASSIGNED = "vendor.assigned";

    private final ResilientKafkaService kafka;
    private final ObjectMapper objectMapper;

    public void publishAssigned(EventBookingVendor a, PartnerVendor v, EventBooking b) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("assignmentId", a.getId().toString());
            payload.put("eventBookingId", a.getEventBookingId().toString());
            payload.put("bookingRef", b.getBookingRef());
            payload.put("vendor", Map.of(
                    "id", v.getId().toString(),
                    "businessName", nz(v.getBusinessName()),
                    "ownerName", nz(v.getOwnerName()),
                    "phone", nz(v.getPhone()),
                    "email", nz(v.getEmail()),
                    "whatsapp", nz(v.getWhatsapp() != null ? v.getWhatsapp() : v.getPhone()),
                    "serviceType", v.getServiceType().name()
            ));
            payload.put("customer", Map.of(
                    "name", nz(b.getCustomerName()),
                    "phone", nz(b.getCustomerPhone()),
                    "email", nz(b.getCustomerEmail())
            ));
            payload.put("event", Map.of(
                    "type", nz(b.getEventType()),
                    "date", b.getEventDate() != null ? b.getEventDate().toString() : "",
                    "time", nz(b.getEventTime()),
                    "durationHours", b.getDurationHours() == null ? 0 : b.getDurationHours(),
                    "guestCount", b.getGuestCount() == null ? 0 : b.getGuestCount(),
                    "venueAddress", nz(b.getVenueAddress()),
                    "city", nz(b.getCity()),
                    "specialRequests", nz(b.getSpecialRequests())
            ));
            payload.put("payoutPaise", a.getPayoutPaise() == null ? 0L : a.getPayoutPaise());

            kafka.send(TOPIC_VENDOR_ASSIGNED, a.getEventBookingId().toString(),
                    objectMapper.writeValueAsString(payload));
            log.info("Published vendor.assigned for booking {} -> vendor {}", b.getBookingRef(), v.getBusinessName());
        } catch (Exception e) {
            log.error("Failed to publish vendor.assigned for assignment {}", a.getId(), e);
        }
    }

    private String nz(String s) { return s == null ? "" : s; }
}
