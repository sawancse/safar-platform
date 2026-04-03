package com.safar.chef.service;

import com.safar.chef.entity.ChefBooking;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.repository.ChefBookingRepository;
import com.safar.chef.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveTrackingService {

    private final ChefBookingRepository bookingRepo;
    private final ChefProfileRepository chefProfileRepo;

    @Transactional
    public ChefBooking updateLocation(UUID chefUserId, UUID bookingId, Double lat, Double lng, Integer etaMinutes) {
        ChefProfile chef = chefProfileRepo.findByUserId(chefUserId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

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
