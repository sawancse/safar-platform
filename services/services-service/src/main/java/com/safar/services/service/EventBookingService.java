package com.safar.services.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.services.dto.CreateEventBookingRequest;
import com.safar.services.dto.ModifyEventBookingRequest;
import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.EventBooking;
import com.safar.services.entity.EventBookingVendor;
import com.safar.services.entity.EventPricingDefault;
import com.safar.services.entity.PartnerVendor;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.EventBookingStatus;
import com.safar.services.entity.enums.ServiceListingStatus;
import com.safar.services.entity.enums.VendorAssignmentStatus;
import com.safar.services.repository.ChefProfileRepository;
import com.safar.services.repository.EventBookingRepository;
import com.safar.services.repository.EventBookingVendorRepository;
import com.safar.services.repository.EventPricingDefaultRepository;
import com.safar.services.repository.PartnerVendorRepository;
import com.safar.services.repository.ServiceListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingService {

    private final EventBookingRepository eventRepo;
    private final ChefProfileRepository chefProfileRepo;
    private final EventPricingDefaultRepository eventPricingRepo;
    private final KafkaTemplate<String, String> kafka;
    private final PartnerVendorRepository partnerVendorRepo;
    private final ServiceListingRepository serviceListingRepo;
    private final EventBookingVendorRepository eventBookingVendorRepo;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Bespoke service flows (singer / decor / pandit / cake / appliance /
     * staff-hire) embed their own pricing in menuDescription under a
     * `breakdown` object along with a `type` discriminator. Extract it so
     * the backend can trust the frontend's total instead of running the
     * chef-centric per-plate math.
     */
    private record BespokeBreakdown(String type, long total, long advance) {}

    private BespokeBreakdown parseBespokeBreakdown(String menuDescription) {
        if (menuDescription == null || menuDescription.isBlank()) return null;
        try {
            Map<String, Object> md = JSON.readValue(menuDescription, new TypeReference<Map<String, Object>>() {});
            Object typeObj = md.get("type");
            if (!(typeObj instanceof String type)) return null;
            // Only known bespoke types trigger the override
            switch (type) {
                case "LIVE_MUSIC":
                case "EVENT_DECOR":
                case "PANDIT_PUJA":
                case "DESIGNER_CAKE":
                case "APPLIANCE_RENTAL":
                case "STAFF_HIRE":
                    break;
                default:
                    return null;
            }
            Object breakdownObj = md.get("breakdown");
            if (!(breakdownObj instanceof Map<?, ?> b)) return null;
            long total   = b.get("total")   instanceof Number n ? n.longValue() : 0L;
            long advance = b.get("advance") instanceof Number n ? n.longValue() : 0L;
            if (total <= 0) return null;
            return new BespokeBreakdown(type, total, advance);
        } catch (Exception e) {
            log.warn("Failed to parse bespoke breakdown from menuDescription: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sum the indicative estimates from servicesJson. Each entry looks like
     *   {"key":"photography","label":"Photographer","estPaise":1500000,...}
     * and we treat estPaise as the midpoint of the price range the frontend
     * showed the customer. Returns 0 if absent or unparseable — services
     * are optional and we never fail a booking over malformed estimates.
     */
    private long computeServicesEstimatePaise(String servicesJson) {
        if (servicesJson == null || servicesJson.isBlank()) return 0L;
        try {
            List<Map<String, Object>> list = JSON.readValue(
                servicesJson, new TypeReference<List<Map<String, Object>>>() {}
            );
            if (list == null) return 0L;
            long total = 0L;
            for (Map<String, Object> svc : list) {
                Object est = svc.get("estPaise");
                if (est instanceof Number) total += ((Number) est).longValue();
            }
            return total;
        } catch (Exception e) {
            log.warn("Failed to parse servicesJson for estimate: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Compute staff cost from a JSON map of role → count, e.g.
     *   {"waiter": 2, "cleaner": 1, "bartender": 1}
     * Rates are looked up from STAFF_ROLE rows in event_pricing_defaults so
     * they remain configurable. Returns 0 on any parse/lookup failure rather
     * than failing the booking.
     */
    private long computeStaffPaiseFromRoles(String staffRolesJson) {
        if (staffRolesJson == null || staffRolesJson.isBlank()) return 0L;
        try {
            Map<String, Integer> roleCounts = JSON.readValue(
                staffRolesJson, new TypeReference<Map<String, Integer>>() {}
            );
            if (roleCounts == null || roleCounts.isEmpty()) return 0L;
            List<EventPricingDefault> roles = eventPricingRepo
                .findByCategoryAndActiveTrueOrderBySortOrderAsc("STAFF_ROLE");
            long total = 0L;
            for (EventPricingDefault role : roles) {
                Integer count = roleCounts.get(role.getItemKey());
                if (count != null && count > 0) {
                    total += (long) count * role.getDefaultPricePaise();
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("Failed to parse staffRolesJson='{}': {}", staffRolesJson, e.getMessage());
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public Page<EventBooking> browseEvents(Pageable pageable) {
        return eventRepo.findAll(pageable);
    }

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
        ChefProfile chef = null;
        if (req.chefId() != null) {
            chef = chefProfileRepo.findById(req.chefId()).orElse(null);
        }

        int guestCount = req.guestCount() != null ? req.guestCount() : 50;

        // Bespoke service flows (singer / decor / pandit / cake / appliance /
        // staff-hire) embed a `breakdown` object in menuDescription with the
        // authoritative total the customer already saw. Trust it when present
        // instead of running the chef-centric per-plate math which would
        // compute ₹300 for a ₹12,000 singer booking.
        BespokeBreakdown bespoke = parseBespokeBreakdown(req.menuDescription());

        // Calculate food cost: use chef's eventMinPlatePaise or default ₹300/plate.
        // Bespoke service bookings have no chef / food component — zero it out.
        long pricePerPlate = chef != null && chef.getEventMinPlatePaise() != null
                ? chef.getEventMinPlatePaise() : 30000L;
        long totalFoodPaise = bespoke != null ? 0L : pricePerPlate * guestCount;

        // Add-ons
        long decorationPaise = Boolean.TRUE.equals(req.decorationRequired()) ? DECORATION_DEFAULT_PAISE : 0L;
        long cakePaise = Boolean.TRUE.equals(req.cakeRequired()) ? CAKE_DEFAULT_PAISE : 0L;
        long staffPaise = computeStaffPaiseFromRoles(req.staffRolesJson());
        if (staffPaise == 0L) {
            int staffCount = Boolean.TRUE.equals(req.staffRequired()) && req.staffCount() != null ? req.staffCount() : 0;
            staffPaise = staffCount * STAFF_PER_PERSON_PAISE;
        }

        // Indicative partner-service estimate. Stored in otherAddonsPaise so the
        // existing bookkeeping lines (totalAmountPaise, platformFeePaise, etc.)
        // pick it up automatically. Chef will reconcile with the real vendor
        // quote before the customer pays the balance.
        long servicesEstPaise = computeServicesEstimatePaise(req.servicesJson());

        long totalAmountPaise, advanceAmountPaise, balanceAmountPaise;
        if (bespoke != null) {
            // Frontend already computed total; trust it.
            totalAmountPaise   = bespoke.total;
            advanceAmountPaise = bespoke.advance > 0 ? bespoke.advance : totalAmountPaise * 60 / 100;
            balanceAmountPaise = totalAmountPaise - advanceAmountPaise;
            // Route the money into the line-item field that best matches the
            // type so the admin expandable row shows it in a sensible place.
            decorationPaise = 0L;
            cakePaise       = 0L;
            staffPaise      = 0L;
            servicesEstPaise = 0L;
            switch (bespoke.type) {
                case "EVENT_DECOR":      decorationPaise  = bespoke.total; break;
                case "DESIGNER_CAKE":    cakePaise        = bespoke.total; break;
                case "STAFF_HIRE":       staffPaise       = bespoke.total; break;
                case "LIVE_MUSIC":
                case "PANDIT_PUJA":
                case "APPLIANCE_RENTAL":
                default:                 servicesEstPaise = bespoke.total; break;
            }
        } else {
            totalAmountPaise   = totalFoodPaise + decorationPaise + cakePaise + staffPaise + servicesEstPaise;
            advanceAmountPaise = totalAmountPaise * 50 / 100;
            balanceAmountPaise = totalAmountPaise - advanceAmountPaise;
        }
        long platformFeePaise = totalAmountPaise * 15 / 100;
        long chefEarningsPaise = totalAmountPaise - platformFeePaise;

        EventBooking event = EventBooking.builder()
                .bookingRef(generateEventRef())
                .chefId(chef != null ? chef.getId() : null)
                .customerId(customerId)
                .chefName(chef != null ? chef.getName() : null)
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
                .otherAddonsPaise(servicesEstPaise)
                .totalAmountPaise(totalAmountPaise)
                .advanceAmountPaise(advanceAmountPaise)
                .balanceAmountPaise(balanceAmountPaise)
                .platformFeePaise(platformFeePaise)
                .chefEarningsPaise(chefEarningsPaise)
                .specialRequests(req.specialRequests())
                .servicesJson(req.servicesJson())
                .staffRolesJson(req.staffRolesJson())
                .panditCount(req.panditCount() != null && req.panditCount() > 0 ? req.panditCount() : 1)
                .samagriTier(req.samagriTier())
                // Bespoke service bookings (singer / decor / cake / pandit /
                // staff / appliance) ship with a fixed catalog price — there's
                // no chef quote step. Skip INQUIRY → QUOTED → CONFIRMED and
                // mark them CONFIRMED on creation so the customer can pay the
                // advance immediately. Traditional cook events still start
                // at INQUIRY and wait for the chef to quote.
                .status(bespoke != null ? EventBookingStatus.CONFIRMED : EventBookingStatus.INQUIRY)
                .confirmedAt(bespoke != null ? OffsetDateTime.now() : null)
                .startJobOtp(bespoke != null ? String.format("%04d", new java.security.SecureRandom().nextInt(10000)) : null)
                .build();

        EventBooking saved = eventRepo.save(event);
        log.info("Event booking created: {} ref={} chef={} customer={} status={}", saved.getId(), saved.getBookingRef(), chef != null ? chef.getId() : "unassigned", customerId, saved.getStatus());

        try {
            kafka.send("event.booking.created", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.created Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public EventBooking quoteEvent(UUID chefId, UUID eventId, Long totalAmountPaise) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        chef.ensureNotSuspended();

        if (event.getChefId() != null && !event.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to quote this event");
        }
        // If no chef assigned yet (inquiry), assign this chef
        if (event.getChefId() == null) {
            event.setChefId(chef.getId());
            event.setChefName(chef.getName());
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

        EventBooking saved = eventRepo.save(event);
        log.info("Event booking quoted: {} totalPaise={}", eventId, totalAmountPaise);

        try {
            kafka.send("event.booking.quoted", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.quoted Kafka event: {}", e.getMessage());
        }

        return saved;
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
        if (event.getStartJobOtp() == null) {
            event.setStartJobOtp(String.format("%04d", new java.security.SecureRandom().nextInt(10000)));
        }
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking confirmed: {}", eventId);

        try {
            kafka.send("event.booking.confirmed", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.confirmed Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public EventBooking markAdvancePaid(UUID customerId, UUID eventId,
                                        String razorpayOrderId, String razorpayPaymentId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only pay advance for your own event bookings");
        }

        if (event.getStatus() != EventBookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Event must be CONFIRMED before marking advance paid");
        }

        // Razorpay handles are mandatory — without them we'd be flipping the booking
        // to ADVANCE_PAID (and emailing "Payment Received") on the caller's say-so.
        // Both are issued by Razorpay's checkout success callback, so a legit caller
        // always has them; a direct-API / admin-impersonation caller skipping payment
        // will not. Mirrors payBalance().
        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "razorpayOrderId and razorpayPaymentId are required to record advance payment");
        }
        event.setAdvanceRazorpayOrderId(razorpayOrderId);
        event.setAdvanceRazorpayPaymentId(razorpayPaymentId);

        event.setStatus(EventBookingStatus.ADVANCE_PAID);
        // Backstop: if a CONFIRMED booking somehow lacks the start-job OTP (legacy
        // rows, manual admin confirms, or a creation race), mint one here so the
        // customer always has something to share with the partner on the day.
        if (event.getStartJobOtp() == null || event.getStartJobOtp().isBlank()) {
            event.setStartJobOtp(String.format("%04d", new java.security.SecureRandom().nextInt(10000)));
        }
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking advance paid: {} razorpayPaymentId={}", eventId, razorpayPaymentId);

        try {
            kafka.send("event.booking.advance.paid", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.advance.paid Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public EventBooking startJob(UUID chefId, UUID eventId, String otp) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        chef.ensureNotSuspended();

        if (event.getChefId() == null || !event.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to start this event");
        }
        // Allow CONFIRMED/ADVANCE_PAID → IN_PROGRESS, and also let an IN_PROGRESS event
        // whose jobStartedAt is null (legacy / stuck rows) be recovered via the OTP path.
        boolean recoverableInProgress = event.getStatus() == EventBookingStatus.IN_PROGRESS
                && event.getJobStartedAt() == null;
        if (event.getStatus() != EventBookingStatus.CONFIRMED
                && event.getStatus() != EventBookingStatus.ADVANCE_PAID
                && !recoverableInProgress) {
            throw new IllegalArgumentException("Event must be CONFIRMED or ADVANCE_PAID to start");
        }
        if (event.getStartJobOtp() == null || !event.getStartJobOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        event.setStatus(EventBookingStatus.IN_PROGRESS);
        event.setJobStartedAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking job started: {}", eventId);
        return saved;
    }

    @Transactional
    public EventBooking payBalance(UUID customerId, UUID eventId,
                                    String razorpayOrderId, String razorpayPaymentId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authorized to pay this event");
        }
        if (event.getBalancePaidAt() != null) {
            throw new IllegalArgumentException("Balance already paid");
        }
        if (event.getStatus() == EventBookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay a cancelled event");
        }
        // Razorpay handles are mandatory — without them we'd be flipping
        // balance_paid_at on the customer's say-so. Both are issued by
        // Razorpay's checkout success callback, so a legit caller always
        // has them; a direct-API caller skipping payment will not.
        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "razorpayOrderId and razorpayPaymentId are required to record balance payment");
        }

        event.setBalanceRazorpayOrderId(razorpayOrderId);
        event.setBalanceRazorpayPaymentId(razorpayPaymentId);
        event.setBalancePaidAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking balance paid: {} razorpayPaymentId={}", eventId, razorpayPaymentId);
        return saved;
    }

    @Transactional
    public EventBooking completeEvent(UUID chefId, UUID eventId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        chef.ensureNotSuspended();

        if (event.getChefId() == null || !event.getChefId().equals(chef.getId())) {
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

        EventBooking saved = eventRepo.save(event);
        log.info("Event booking completed: {}", eventId);

        try {
            kafka.send("event.booking.completed", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.completed Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public EventBooking cancelEvent(UUID userId, UUID eventId, String reason) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        ChefProfile chefProfile = chefProfileRepo.findByUserId(userId).orElse(null);
        boolean isChef = chefProfile != null && event.getChefId() != null && event.getChefId().equals(chefProfile.getId());
        boolean isCustomer = event.getCustomerId().equals(userId);

        if (!isChef && !isCustomer) {
            throw new IllegalArgumentException("Not authorized to cancel this event");
        }
        if (isChef) {
            chefProfile.ensureNotSuspended();
        }
        if (event.getStatus() == EventBookingStatus.CANCELLED || event.getStatus() == EventBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Event cannot be cancelled in current status");
        }

        event.setStatus(EventBookingStatus.CANCELLED);
        event.setCancellationReason(reason);
        event.setCancelledAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking cancelled: {} by userId={}", eventId, userId);

        try {
            kafka.send("event.booking.cancelled", saved.getId().toString(), buildEventJson(saved));
        } catch (Exception e) {
            log.warn("Failed to send event.booking.cancelled Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public EventBooking rateEvent(UUID customerId, UUID eventId, int rating, String comment) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to rate this event");
        }
        // Rating unlocks in two cases:
        //   1. Partner has marked the job COMPLETED (the canonical path)
        //   2. Status is ADVANCE_PAID/IN_PROGRESS AND the event date is in the
        //      past — partners frequently forget to mark complete, and we want
        //      the customer to be able to leave a review the day after the
        //      event without chasing the partner.
        // Pre-event states (INQUIRY/QUOTED/CONFIRMED/PENDING*) and CANCELLED
        // are still rejected.
        boolean isCompleted = event.getStatus() == EventBookingStatus.COMPLETED;
        boolean eventHappened =
                (event.getStatus() == EventBookingStatus.ADVANCE_PAID
                        || event.getStatus() == EventBookingStatus.IN_PROGRESS)
                && event.getEventDate() != null
                && event.getEventDate().isBefore(java.time.LocalDate.now().plusDays(1)); // today or earlier
        if (!isCompleted && !eventHappened) {
            throw new IllegalArgumentException("Rating unlocks after the event has been delivered");
        }
        if (event.getRatingGiven() != null) {
            throw new IllegalArgumentException("Event already rated");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        event.setRatingGiven(rating);
        event.setReviewComment(comment);

        // For chef-assigned bookings, update the chef's running average. Skip
        // gracefully when the lookup fails so a missing/legacy chef row never
        // blocks the customer from leaving a review.
        if (event.getChefId() != null) {
            chefProfileRepo.findById(event.getChefId()).ifPresent(chef -> {
                double currentTotal = chef.getRating() * chef.getReviewCount();
                int newCount = chef.getReviewCount() + 1;
                double newRating = (currentTotal + rating) / newCount;
                chef.setRating(Math.round(newRating * 10.0) / 10.0);
                chef.setReviewCount(newCount);
                chefProfileRepo.save(chef);
            });
        }

        // Self-service vendor bookings (pandit / cake / decor / singer / staff /
        // appliance) have chef_id = NULL and route through EventBookingVendor.
        // Update both the PartnerVendor's running rating and — when the vendor
        // came in via the self-service onboarding wizard — the linked
        // ServiceListing's avgRating, so the public storefront reflects it.
        eventBookingVendorRepo.findFirstByEventBookingIdAndStatusNot(eventId, VendorAssignmentStatus.CANCELLED)
                .ifPresent(assignment -> {
                    partnerVendorRepo.findById(assignment.getVendorId()).ifPresent(vendor -> {
                        java.math.BigDecimal currentAvg = vendor.getRatingAvg() != null
                                ? vendor.getRatingAvg() : java.math.BigDecimal.ZERO;
                        int currentCount = vendor.getRatingCount() != null ? vendor.getRatingCount() : 0;
                        java.math.BigDecimal total = currentAvg.multiply(java.math.BigDecimal.valueOf(currentCount))
                                .add(java.math.BigDecimal.valueOf(rating));
                        int newCount = currentCount + 1;
                        java.math.BigDecimal newAvg = total.divide(
                                java.math.BigDecimal.valueOf(newCount),
                                2, java.math.RoundingMode.HALF_UP);
                        vendor.setRatingAvg(newAvg);
                        vendor.setRatingCount(newCount);
                        partnerVendorRepo.save(vendor);

                        if (vendor.getServiceListingId() != null) {
                            serviceListingRepo.findById(vendor.getServiceListingId()).ifPresent(listing -> {
                                java.math.BigDecimal listingAvg = listing.getAvgRating() != null
                                        ? listing.getAvgRating() : java.math.BigDecimal.ZERO;
                                int listingCount = listing.getRatingCount() != null ? listing.getRatingCount() : 0;
                                java.math.BigDecimal listingTotal = listingAvg.multiply(java.math.BigDecimal.valueOf(listingCount))
                                        .add(java.math.BigDecimal.valueOf(rating));
                                int newListingCount = listingCount + 1;
                                java.math.BigDecimal newListingAvg = listingTotal.divide(
                                        java.math.BigDecimal.valueOf(newListingCount),
                                        2, java.math.RoundingMode.HALF_UP);
                                listing.setAvgRating(newListingAvg);
                                listing.setRatingCount(newListingCount);
                                serviceListingRepo.save(listing);
                            });
                        }
                    });
                });

        log.info("Event booking rated: {} rating={} chef={} hasVendor={}",
                eventId, rating, event.getChefId(),
                event.getChefId() == null);
        return eventRepo.save(event);
    }

    @Transactional
    public EventBooking modifyEventBooking(UUID customerId, UUID eventId, ModifyEventBookingRequest req) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));

        if (!event.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to modify this event");
        }
        if (event.getStatus() != EventBookingStatus.INQUIRY && event.getStatus() != EventBookingStatus.QUOTED) {
            throw new IllegalArgumentException("Event can only be modified in INQUIRY or QUOTED status");
        }

        // Apply non-null fields
        if (req.eventDate() != null) event.setEventDate(req.eventDate());
        if (req.eventTime() != null) event.setEventTime(req.eventTime());
        if (req.durationHours() != null) event.setDurationHours(req.durationHours());
        if (req.venueAddress() != null) event.setVenueAddress(req.venueAddress());
        if (req.city() != null) event.setCity(req.city());
        if (req.locality() != null) event.setLocality(req.locality());
        if (req.pincode() != null) event.setPincode(req.pincode());
        if (req.menuDescription() != null) event.setMenuDescription(req.menuDescription());
        if (req.cuisinePreferences() != null) event.setCuisinePreferences(req.cuisinePreferences());
        if (req.specialRequests() != null) event.setSpecialRequests(req.specialRequests());
        if (req.servicesJson() != null) event.setServicesJson(req.servicesJson());

        // If guest count or add-ons changed, recalculate pricing
        boolean recalculate = false;
        if (req.guestCount() != null) { event.setGuestCount(req.guestCount()); recalculate = true; }
        if (req.decorationRequired() != null) {
            event.setDecorationPaise(Boolean.TRUE.equals(req.decorationRequired()) ? DECORATION_DEFAULT_PAISE : 0L);
            recalculate = true;
        }
        if (req.cakeRequired() != null) {
            event.setCakePaise(Boolean.TRUE.equals(req.cakeRequired()) ? CAKE_DEFAULT_PAISE : 0L);
            recalculate = true;
        }
        if (req.staffRolesJson() != null) {
            event.setStaffRolesJson(req.staffRolesJson());
            long staffPaise = computeStaffPaiseFromRoles(req.staffRolesJson());
            if (staffPaise == 0L && (req.staffRequired() != null || req.staffCount() != null)) {
                int staffCount = Boolean.TRUE.equals(req.staffRequired()) && req.staffCount() != null ? req.staffCount() : 0;
                staffPaise = staffCount * STAFF_PER_PERSON_PAISE;
            }
            event.setStaffPaise(staffPaise);
            recalculate = true;
        } else if (req.staffRequired() != null || req.staffCount() != null) {
            int staffCount = Boolean.TRUE.equals(req.staffRequired()) && req.staffCount() != null ? req.staffCount() : 0;
            event.setStaffPaise(staffCount * STAFF_PER_PERSON_PAISE);
            recalculate = true;
        }

        if (recalculate) {
            long totalFoodPaise = event.getPricePerPlatePaise() * event.getGuestCount();
            event.setTotalFoodPaise(totalFoodPaise);

            long totalAmountPaise = totalFoodPaise + event.getDecorationPaise() + event.getCakePaise()
                    + event.getStaffPaise() + event.getOtherAddonsPaise();
            long advanceAmountPaise = totalAmountPaise * 50 / 100;
            long balanceAmountPaise = totalAmountPaise - advanceAmountPaise;
            long platformFeePaise = totalAmountPaise * 15 / 100;
            long chefEarningsPaise = totalAmountPaise - platformFeePaise;

            event.setTotalAmountPaise(totalAmountPaise);
            event.setAdvanceAmountPaise(advanceAmountPaise);
            event.setBalanceAmountPaise(balanceAmountPaise);
            event.setPlatformFeePaise(platformFeePaise);
            event.setChefEarningsPaise(chefEarningsPaise);
        }

        // If was QUOTED and customer modifies, reset to INQUIRY so chef re-quotes
        if (event.getStatus() == EventBookingStatus.QUOTED) {
            event.setStatus(EventBookingStatus.INQUIRY);
            event.setQuotedAt(null);
        }

        event.setModifiedAt(OffsetDateTime.now());
        event.setModificationCount(event.getModificationCount() != null ? event.getModificationCount() + 1 : 1);

        EventBooking saved = eventRepo.save(event);
        log.info("Event booking modified: {} by customer={}", eventId, customerId);
        return saved;
    }

    @Transactional
    public EventBooking adminCancelEvent(UUID eventId, String reason) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
        if (event.getStatus() == EventBookingStatus.CANCELLED || event.getStatus() == EventBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Event cannot be cancelled in current status");
        }
        event.setStatus(EventBookingStatus.CANCELLED);
        event.setCancellationReason(reason != null ? reason : "Cancelled by admin");
        event.setCancelledAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Admin cancelled event booking: {}", eventId);
        try { kafka.send("event.booking.cancelled", saved.getId().toString(), buildEventJson(saved)); }
        catch (Exception e) { log.warn("Kafka event.booking.cancelled failed: {}", e.getMessage()); }
        return saved;
    }

    @Transactional
    public EventBooking adminCompleteEvent(UUID eventId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
        if (event.getStatus() == EventBookingStatus.COMPLETED || event.getStatus() == EventBookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Event cannot be completed in current status");
        }
        event.setStatus(EventBookingStatus.COMPLETED);
        event.setCompletedAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Admin completed event booking: {}", eventId);
        try { kafka.send("event.booking.completed", saved.getId().toString(), buildEventJson(saved)); }
        catch (Exception e) { log.warn("Kafka event.booking.completed failed: {}", e.getMessage()); }
        return saved;
    }

    @Transactional
    public EventBooking adminAssignChef(UUID eventId, UUID chefId) {
        EventBooking event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
        ChefProfile chef = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        event.setChefId(chef.getId());
        event.setChefName(chef.getName());

        // Recalculate with chef's plate price if available
        if (chef.getEventMinPlatePaise() != null) {
            event.setPricePerPlatePaise(chef.getEventMinPlatePaise());
            long totalFoodPaise = chef.getEventMinPlatePaise() * event.getGuestCount();
            event.setTotalFoodPaise(totalFoodPaise);
            long totalAmountPaise = totalFoodPaise + event.getDecorationPaise() + event.getCakePaise()
                    + event.getStaffPaise() + event.getOtherAddonsPaise();
            event.setTotalAmountPaise(totalAmountPaise);
            event.setAdvanceAmountPaise(totalAmountPaise * 50 / 100);
            event.setBalanceAmountPaise(totalAmountPaise - event.getAdvanceAmountPaise());
            event.setPlatformFeePaise(totalAmountPaise * 15 / 100);
            event.setChefEarningsPaise(totalAmountPaise - event.getPlatformFeePaise());
        }

        log.info("Admin assigned chef {} to event {}", chefId, eventId);
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

    // ── V28: vendor-side queries (self-service ServiceListing vendors) ──────

    /** All bookings currently assigned to this vendor (via PartnerVendor stub). */
    @Transactional(readOnly = true)
    public List<EventBooking> getMyVendorBookings(UUID vendorUserId) {
        return eventRepo.findAssignedToVendorUser(vendorUserId);
    }

    /**
     * Open inquiries this vendor could fulfill. Derives the vendor's eligible
     * service-types + cities from their VERIFIED ServiceListings, then asks the
     * repo for unassigned bookings matching either filter.
     */
    @Transactional(readOnly = true)
    public List<EventBooking> getOpenInquiriesForVendor(UUID vendorUserId) {
        List<ServiceListing> myListings = serviceListingRepo.findByVendorUserId(vendorUserId).stream()
                .filter(l -> l.getStatus() == ServiceListingStatus.VERIFIED)
                .toList();
        if (myListings.isEmpty()) return List.of();

        // Collect distinct menuDescription.type strings the vendor can serve
        java.util.Set<String> typesSet = new java.util.HashSet<>();
        java.util.Set<String> citiesSet = new java.util.HashSet<>();
        boolean serveEverywhere = false;
        for (ServiceListing l : myListings) {
            String mappedType = mapListingTypeToMenuType(l.getServiceType());
            if (mappedType != null) typesSet.add(mappedType);
            if (l.getCities() != null && !l.getCities().isEmpty()) {
                for (String c : l.getCities()) {
                    if (c != null && !c.isBlank()) citiesSet.add(c.toLowerCase());
                }
            } else if (l.getHomeCity() != null && !l.getHomeCity().isBlank()) {
                citiesSet.add(l.getHomeCity().toLowerCase());
            } else {
                // No city info → treat as serve-everywhere for this listing
                serveEverywhere = true;
            }
        }
        if (typesSet.isEmpty()) return List.of();

        String[] types  = typesSet.toArray(new String[0]);
        String[] cities = serveEverywhere ? new String[0] : citiesSet.toArray(new String[0]);
        return eventRepo.findOpenInquiries(types, cities);
    }

    /**
     * Self-service vendor marks the booking complete after job is done.
     * Mirrors {@link #completeEvent} but authorizes via PartnerVendor.
     */
    @Transactional
    public EventBooking completeAsVendor(UUID vendorUserId, UUID bookingId) {
        EventBooking event = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
        PartnerVendor stub = partnerVendorRepo.findByUserId(vendorUserId)
                .orElseThrow(() -> new IllegalStateException("No vendor profile found"));

        boolean assigned = eventBookingVendorRepo.findByEventBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .anyMatch(v -> v.getVendorId().equals(stub.getId())
                        && v.getStatus() != VendorAssignmentStatus.CANCELLED);
        if (!assigned) throw new IllegalStateException("You are not assigned to this booking");

        if (event.getStatus() != EventBookingStatus.ADVANCE_PAID
                && event.getStatus() != EventBookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Event must be ADVANCE_PAID or IN_PROGRESS to complete");
        }

        event.setStatus(EventBookingStatus.COMPLETED);
        event.setCompletedAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking {} completed by vendor user={} stub={}", bookingId, vendorUserId, stub.getId());
        return saved;
    }

    /**
     * Self-service vendor (pandit/decor/cake/singer/staff) starts the job by
     * entering the OTP customer shared with them. Mirrors {@link #startJob}
     * but authorizes via PartnerVendor + event_booking_vendor rather than
     * ChefProfile (vendors don't have chef profiles).
     */
    @Transactional
    public EventBooking startJobAsVendor(UUID vendorUserId, UUID bookingId, String otp) {
        EventBooking event = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Event booking not found"));
        PartnerVendor stub = partnerVendorRepo.findByUserId(vendorUserId)
                .orElseThrow(() -> new IllegalStateException("No vendor profile found"));

        boolean assigned = eventBookingVendorRepo.findByEventBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .anyMatch(v -> v.getVendorId().equals(stub.getId())
                        && v.getStatus() != VendorAssignmentStatus.CANCELLED);
        if (!assigned) throw new IllegalStateException("You are not assigned to this booking");

        boolean recoverableInProgress = event.getStatus() == EventBookingStatus.IN_PROGRESS
                && event.getJobStartedAt() == null;
        if (event.getStatus() != EventBookingStatus.CONFIRMED
                && event.getStatus() != EventBookingStatus.ADVANCE_PAID
                && !recoverableInProgress) {
            throw new IllegalArgumentException("Event must be CONFIRMED or ADVANCE_PAID to start");
        }
        if (event.getStartJobOtp() == null || !event.getStartJobOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        event.setStatus(EventBookingStatus.IN_PROGRESS);
        event.setJobStartedAt(OffsetDateTime.now());
        EventBooking saved = eventRepo.save(event);
        log.info("Event booking {} started by vendor user={} stub={}", bookingId, vendorUserId, stub.getId());
        return saved;
    }

    /**
     * Vendor pushes their current geolocation + ETA to a booking they're
     * assigned to. Authorization: caller must own a PartnerVendor row that is
     * actively assigned to this booking. Customer's tracking panel polls these
     * fields — works the same way it does for cook bookings.
     */
    @Transactional
    public EventBooking updateVendorLocation(UUID vendorUserId, UUID bookingId,
                                              Double lat, Double lng, Integer etaMinutes) {
        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        PartnerVendor stub = partnerVendorRepo.findByUserId(vendorUserId)
                .orElseThrow(() -> new IllegalStateException("No vendor profile found for current user"));

        boolean assigned = eventBookingVendorRepo.findByEventBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .anyMatch(v -> v.getVendorId().equals(stub.getId())
                        && v.getStatus() != VendorAssignmentStatus.CANCELLED);
        if (!assigned) {
            throw new IllegalStateException("You are not assigned to this booking");
        }

        booking.setChefLat(lat);
        booking.setChefLng(lng);
        booking.setEtaMinutes(etaMinutes);
        booking.setLocationUpdatedAt(OffsetDateTime.now());
        return eventRepo.save(booking);
    }

    /**
     * Self-claim an open inquiry. Idempotent — if the vendor already has a row
     * on this booking, returns it. Refuses if a different vendor has already
     * claimed (active row exists) or if the booking has a chef assigned.
     */
    @Transactional
    public EventBookingVendor claimBooking(UUID vendorUserId, UUID bookingId) {
        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (booking.getChefId() != null) {
            throw new IllegalStateException("Booking already has a chef assigned");
        }
        PartnerVendor stub = partnerVendorRepo.findByUserId(vendorUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "No vendor profile found — your service listing must be approved first"));

        // Already claimed by someone (including possibly self)?
        for (EventBookingVendor existing : eventBookingVendorRepo.findByEventBookingIdOrderByCreatedAtDesc(bookingId)) {
            if (existing.getStatus() != VendorAssignmentStatus.CANCELLED) {
                if (existing.getVendorId().equals(stub.getId())) return existing;
                throw new IllegalStateException("Booking already claimed by another vendor");
            }
        }

        EventBookingVendor row = EventBookingVendor.builder()
                .eventBookingId(bookingId)
                .vendorId(stub.getId())
                .status(VendorAssignmentStatus.ASSIGNED)
                .assignedAt(OffsetDateTime.now())
                .adminNotes("Self-claimed by vendor")
                .build();
        EventBookingVendor saved = eventBookingVendorRepo.save(row);
        log.info("Vendor user={} stub={} self-claimed booking={}", vendorUserId, stub.getId(), bookingId);
        return saved;
    }

    /** Inverse of ServiceListingService.mapToVendorType — listing type → menuDescription.type. */
    private static String mapListingTypeToMenuType(String serviceListingType) {
        if (serviceListingType == null) return null;
        return switch (serviceListingType) {
            case "CAKE_DESIGNER" -> "DESIGNER_CAKE";
            case "PANDIT"        -> "PANDIT_PUJA";
            case "DECORATOR"     -> "EVENT_DECOR";
            case "SINGER"        -> "LIVE_MUSIC";
            case "STAFF_HIRE"    -> "STAFF_HIRE";
            default              -> null;
        };
    }

    private String buildEventJson(EventBooking e) {
        return String.format(
                "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"chefId\":\"%s\",\"customerId\":\"%s\","
                + "\"chefName\":\"%s\",\"customerName\":\"%s\",\"eventType\":\"%s\",\"eventDate\":\"%s\","
                + "\"eventTime\":\"%s\",\"guestCount\":%d,\"venueAddress\":\"%s\",\"city\":\"%s\","
                + "\"totalAmountPaise\":%d,\"advanceAmountPaise\":%d,\"balanceAmountPaise\":%d,"
                + "\"durationHours\":%d,\"status\":\"%s\",\"serviceCategory\":\"%s\","
                + "\"customerEmail\":\"%s\",\"customerPhone\":\"%s\"}",
                e.getId(),
                e.getBookingRef() != null ? e.getBookingRef() : "",
                e.getChefId() != null ? e.getChefId() : "",
                e.getCustomerId() != null ? e.getCustomerId() : "",
                e.getChefName() != null ? e.getChefName() : "",
                e.getCustomerName() != null ? e.getCustomerName() : "",
                e.getEventType() != null ? e.getEventType() : "",
                e.getEventDate() != null ? e.getEventDate() : "",
                e.getEventTime() != null ? e.getEventTime() : "",
                e.getGuestCount() != null ? e.getGuestCount() : 0,
                e.getVenueAddress() != null ? e.getVenueAddress().replace("\"", "\\\"") : "",
                e.getCity() != null ? e.getCity() : "",
                e.getTotalAmountPaise() != null ? e.getTotalAmountPaise() : 0,
                e.getAdvanceAmountPaise() != null ? e.getAdvanceAmountPaise() : 0,
                e.getBalanceAmountPaise() != null ? e.getBalanceAmountPaise() : 0,
                e.getDurationHours() != null ? e.getDurationHours() : 0,
                e.getStatus() != null ? e.getStatus() : "",
                extractServiceCategory(e.getMenuDescription()),
                e.getCustomerEmail() != null ? e.getCustomerEmail() : "",
                e.getCustomerPhone() != null ? e.getCustomerPhone() : "");
    }

    /**
     * Pulls the `type` discriminator out of menuDescription JSON so notifications
     * can render copy specific to the service vertical (PANDIT_PUJA, CAKE_DESIGNER,
     * EVENT_DECOR, LIVE_MUSIC, STAFF_HIRE, APPLIANCE_RENTAL, COOK, …). Defaults to
     * empty string when the field is missing or the JSON is unparseable — the
     * consumer treats that as a generic "event" booking.
     */
    private String extractServiceCategory(String menuDescription) {
        if (menuDescription == null || menuDescription.isBlank()) return "";
        try {
            Map<String, Object> md = JSON.readValue(menuDescription, new TypeReference<Map<String, Object>>() {});
            Object t = md.get("type");
            return t == null ? "" : t.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
