package com.safar.chef.service;

import com.safar.chef.dto.CreateEventBookingRequest;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.EventBooking;
import com.safar.chef.entity.enums.EventBookingStatus;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.EventBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingService {

    private final EventBookingRepository eventRepo;
    private final ChefProfileRepository chefProfileRepo;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final long DECORATION_DEFAULT_PAISE = 500000L;  // ₹5,000
    private static final long CAKE_DEFAULT_PAISE = 200000L;        // ₹2,000
    private static final long STAFF_PER_PERSON_PAISE = 150000L;    // ₹1,500

    private String generateEventRef() {
        StringBuilder sb = new StringBuilder("SE-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    @Transactional
    public EventBooking createEventBooking(UUID customerId, CreateEventBookingRequest req) {
        ChefProfile chef = chefProfileRepo.findById(req.chefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        int guestCount = req.guestCount() != null ? req.guestCount() : 50;

        // Calculate food cost: use chef's eventMinPlatePaise as default per-plate price
        long pricePerPlate = chef.getEventMinPlatePaise() != null ? chef.getEventMinPlatePaise() : 0L;
        long totalFoodPaise = pricePerPlate * guestCount;

        // Add-ons
        long decorationPaise = Boolean.TRUE.equals(req.decorationRequired()) ? DECORATION_DEFAULT_PAISE : 0L;
        long cakePaise = Boolean.TRUE.equals(req.cakeRequired()) ? CAKE_DEFAULT_PAISE : 0L;
        int staffCount = Boolean.TRUE.equals(req.staffRequired()) && req.staffCount() != null ? req.staffCount() : 0;
        long staffPaise = staffCount * STAFF_PER_PERSON_PAISE;

        long totalAmountPaise = totalFoodPaise + decorationPaise + cakePaise + staffPaise;
        long advanceAmountPaise = totalAmountPaise * 50 / 100;
        long balanceAmountPaise = totalAmountPaise - advanceAmountPaise;
        long platformFeePaise = totalAmountPaise * 15 / 100;
        long chefEarningsPaise = totalAmountPaise - platformFeePaise;

        EventBooking event = EventBooking.builder()
                .bookingRef(generateEventRef())
                .chefId(chef.getId())
                .customerId(customerId)
                .chefName(chef.getName())
                .customerName(req.customerName())
                .customerPhone(req.customerPhone())
                .customerEmail(req.customerEmail())
                .eventType(req.eventType())
                .eventDate(req.eventDate())
                .eventTime(req.eventTime())
                .durationHours(req.durationHours() != null ? req.durationHours() : 4)
                .guestCount(guestCount)
                .venueAddress(req.venueAddress())
                .city(req.city())
                .locality(req.locality())
                .pincode(req.pincode())
                .menuPackageId(req.menuPackageId())
                .menuDescription(req.menuDescription())
                .cuisinePreferences(req.cuisinePreferences())
                .pricePerPlatePaise(pricePerPlate)
                .totalFoodPaise(totalFoodPaise)
                .decorationPaise(decorationPaise)
                .cakePaise(cakePaise)
                .staffPaise(staffPaise)
                .otherAddonsPaise(0L)
                .totalAmountPaise(totalAmountPaise)
                .advanceAmountPaise(advanceAmountPaise)
                .balanceAmountPaise(balanceAmountPaise)
                .platformFeePaise(platformFeePaise)
                .chefEarningsPaise(chefEarningsPaise)
                .specialRequests(req.specialRequests())
                .status(EventBookingStatus.INQUIRY)
                .build();

        EventBooking saved = eventRepo.save(event);
        log.info("Event booking created: {} ref={} chef={} customer={}", saved.getId(), saved.getBookingRef(), chef.getId(), customerId);
        return saved;
    }

    @Transactional
    public EventBooking quoteEvent(UUID chefId, UUID eventId, Long totalAmountPaise) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (!event.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to quote this event");
        }
        if (event.getStatus() != EventBookingStatus.INQUIRY) {
            throw new IllegalArgumentException("Event must be in INQUIRY status to quote");
        }

        // Recalculate fees based on chef's quoted total
        long platformFeePaise = totalAmountPaise * 15 / 100;
        long chefEarningsPaise = totalAmountPaise - platformFeePaise;
        long advanceAmountPaise = totalAmountPaise * 50 / 100;
        long balanceAmountPaise = totalAmountPaise - advanceAmountPaise;

        event.setTotalAmountPaise(totalAmountPaise);
        event.setPlatformFeePaise(platformFeePaise);
        event.setChefEarningsPaise(chefEarningsPaise);
        event.setAdvanceAmountPaise(advanceAmountPaise);
        event.setBalanceAmountPaise(balanceAmountPaise);
        event.setStatus(EventBookingStatus.QUOTED);
        event.setQuotedAt(OffsetDateTime.now());

        log.info("Event booking quoted: {} totalPaise={}", eventId, totalAmountPaise);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking confirmEvent(UUID customerId, UUID eventId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to confirm this event");
        }
        if (event.getStatus() != EventBookingStatus.QUOTED) {
            throw new IllegalArgumentException("Event must be in QUOTED status to confirm");
        }

        event.setStatus(EventBookingStatus.CONFIRMED);
        event.setConfirmedAt(OffsetDateTime.now());
        log.info("Event booking confirmed: {}", eventId);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking markAdvancePaid(UUID eventId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (event.getStatus() != EventBookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Event must be CONFIRMED before marking advance paid");
        }

        event.setStatus(EventBookingStatus.ADVANCE_PAID);
        log.info("Event booking advance paid: {}", eventId);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking completeEvent(UUID chefId, UUID eventId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (!event.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to complete this event");
        }
        if (event.getStatus() != EventBookingStatus.ADVANCE_PAID && event.getStatus() != EventBookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Event must be ADVANCE_PAID or IN_PROGRESS to complete");
        }

        event.setStatus(EventBookingStatus.COMPLETED);
        event.setCompletedAt(OffsetDateTime.now());

        // Update chef total bookings
        chef.setTotalBookings(chef.getTotalBookings() + 1);
        chefProfileRepo.save(chef);

        log.info("Event booking completed: {}", eventId);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking cancelEvent(UUID userId, UUID eventId, String reason) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chefProfile = chefProfileRepo.findByUserId(userId).orElse(null);
        boolean isChef = chefProfile != null && event.getChefId().equals(chefProfile.getId());
        boolean isCustomer = event.getCustomerId().equals(userId);

        if (!isChef && !isCustomer) {
            throw new IllegalArgumentException("Not authorized to cancel this event");
        }
        if (event.getStatus() == EventBookingStatus.CANCELLED || event.getStatus() == EventBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Event cannot be cancelled in current status");
        }

        event.setStatus(EventBookingStatus.CANCELLED);
        event.setCancellationReason(reason);
        event.setCancelledAt(OffsetDateTime.now());
        log.info("Event booking cancelled: {} by userId={}", eventId, userId);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking rateEvent(UUID customerId, UUID eventId, int rating, String comment) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to rate this event");
        }
        if (event.getStatus() != EventBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only rate completed events");
        }
        if (event.getRatingGiven() != null) {
            throw new IllegalArgumentException("Event already rated");
        }

        event.setRatingGiven(rating);
        event.setReviewComment(comment);

        // Update chef's running average rating
        ChefProfile chef = chefProfileRepo.findById(event.getChefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
        double currentTotal = chef.getRating() * chef.getReviewCount();
        int newCount = chef.getReviewCount() + 1;
        double newRating = (currentTotal + rating) / newCount;
        chef.setRating(Math.round(newRating * 10.0) / 10.0);
        chef.setReviewCount(newCount);
        chefProfileRepo.save(chef);

        log.info("Event booking rated: {} rating={} chef={}", eventId, rating, event.getChefId());
        return eventRepo.save(event);
    }

    @Transactional(readOnly = true)
    public List<EventBooking> getMyEvents(UUID customerId) {
        return eventRepo.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<EventBooking> getChefEvents(UUID chefId) {
        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        return eventRepo.findByChefId(chef.getId());
    }

    @Transactional(readOnly = true)
    public EventBooking getEvent(UUID eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
    }
}
