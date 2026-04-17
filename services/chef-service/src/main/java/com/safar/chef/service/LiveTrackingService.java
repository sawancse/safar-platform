package com.safar.chef.service;

import com.safar.chef.entity.ChefBooking;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.repository.ChefBookingRepository;
import com.safar.chef.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveTrackingService {

    private final ChefBookingRepository bookingRepo;
    private final ChefProfileRepository chefProfileRepo;
    private final RestTemplate restTemplate;

    @Value("${services.messaging-service.url:http://localhost:8091}")
    private String messagingServiceUrl;

    @Transactional
    public ChefBooking updateLocation(UUID chefUserId, UUID bookingId, Double lat, Double lng, Integer etaMinutes) {
        ChefProfile chef = chefProfileRepo.findByUserId(chefUserId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        chef.ensureNotSuspended();

        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (booking.getStatus() != ChefBookingStatus.CONFIRMED && booking.getStatus() != ChefBookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Booking must be CONFIRMED or IN_PROGRESS for tracking");
        }

        booking.setChefLat(lat);
        booking.setChefLng(lng);
        booking.setEtaMinutes(etaMinutes);
        booking.setLocationUpdatedAt(OffsetDateTime.now());

        if (booking.getStatus() == ChefBookingStatus.CONFIRMED) {
            booking.setStatus(ChefBookingStatus.IN_PROGRESS);
        }

        log.info("Chef {} location updated for booking {}: lat={}, lng={}, eta={}min",
                chef.getId(), bookingId, lat, lng, etaMinutes);
        return bookingRepo.save(booking);
    }

    /**
     * Update chef location + send a LOCATION message in the chef-customer chat.
     */
    @Transactional
    public ChefBooking shareLocationInChat(UUID chefUserId, UUID bookingId, Double lat, Double lng, Integer etaMinutes) {
        ChefBooking booking = updateLocation(chefUserId, bookingId, lat, lng, etaMinutes);

        // Send LOCATION message to the customer via messaging-service
        try {
            String label = etaMinutes != null && etaMinutes > 0
                    ? "Chef is " + etaMinutes + " min away"
                    : "Chef shared location";

            Map<String, Object> msgReq = new java.util.HashMap<>();
            msgReq.put("listingId", booking.getId().toString()); // use booking ID as listing context
            msgReq.put("recipientId", booking.getCustomerId().toString());
            msgReq.put("bookingId", booking.getId().toString());
            msgReq.put("content", label);
            msgReq.put("messageType", "LOCATION");
            msgReq.put("latitude", lat);
            msgReq.put("longitude", lng);
            msgReq.put("locationLabel", label);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-User-Id", chefUserId.toString());
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(msgReq, headers);
            restTemplate.postForObject(messagingServiceUrl + "/api/v1/messages/internal", entity, Map.class);
            log.info("Location message sent to customer {} for chef booking {}", booking.getCustomerId(), bookingId);
        } catch (Exception e) {
            log.warn("Failed to send location message for booking {}: {}", bookingId, e.getMessage());
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrackingInfo(UUID bookingId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        return Map.of(
                "bookingId", booking.getId(),
                "status", booking.getStatus().name(),
                "chefLat", booking.getChefLat() != null ? booking.getChefLat() : 0,
                "chefLng", booking.getChefLng() != null ? booking.getChefLng() : 0,
                "etaMinutes", booking.getEtaMinutes() != null ? booking.getEtaMinutes() : -1,
                "locationUpdatedAt", booking.getLocationUpdatedAt() != null ? booking.getLocationUpdatedAt().toString() : ""
        );
    }
}
