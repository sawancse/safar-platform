package com.safar.chef.service;

import com.safar.chef.dto.CreateChefBookingRequest;
import com.safar.chef.dto.ModifyChefBookingRequest;
import com.safar.chef.entity.ChefBooking;
import com.safar.chef.entity.ChefMenu;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.enums.ChefBookingStatus;
import com.safar.chef.entity.enums.MealType;
import com.safar.chef.entity.enums.ServiceType;
import com.safar.chef.entity.enums.VerificationStatus;
import com.safar.chef.repository.ChefBookingRepository;
import com.safar.chef.repository.ChefMenuRepository;
import com.safar.chef.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefBookingService {

    private final ChefBookingRepository bookingRepo;
    private final ChefProfileRepository chefProfileRepo;
    private final ChefMenuRepository menuRepo;
    private final KafkaTemplate<String, String> kafka;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String generateBookingRef() {
        StringBuilder sb = new StringBuilder("SC-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    @Transactional
    public ChefBooking createBooking(UUID customerId, CreateChefBookingRequest req) {
        ChefProfile chef = chefProfileRepo.findById(req.chefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        if (!chef.getAvailable()) {
            throw new IllegalArgumentException("Chef is currently not available");
        }
        if (chef.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Chef is not verified yet");
        }

        int guests = req.guestsCount() != null ? req.guestsCount() : 1;
        int meals = req.numberOfMeals() != null ? req.numberOfMeals() : 1;

        // Calculate total: use menu price if menuId provided, otherwise chef's daily rate
        long totalAmountPaise;
        String menuName = null;
        if (req.menuId() != null) {
            ChefMenu menu = menuRepo.findById(req.menuId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu not found"));
            totalAmountPaise = menu.getPricePerPlatePaise() * guests * meals;
            menuName = menu.getName();
        } else {
            if (chef.getDailyRatePaise() == null) {
                throw new IllegalArgumentException("Chef has no daily rate configured");
            }
            totalAmountPaise = chef.getDailyRatePaise() * guests * meals;
        }

        long platformFeePaise = totalAmountPaise * 15 / 100;
        long chefEarningsPaise = totalAmountPaise - platformFeePaise;

        // 10% advance, 90% balance
        long advanceAmountPaise = Math.max(totalAmountPaise * 10 / 100, 100); // min ₹1
        long balanceAmountPaise = totalAmountPaise - advanceAmountPaise;

        ChefBooking booking = ChefBooking.builder()
                .bookingRef(generateBookingRef())
                .chefId(chef.getId())
                .customerId(customerId)
                .chefName(chef.getName())
                .customerName(req.customerName())
                .serviceType(req.serviceType() != null ? ServiceType.valueOf(req.serviceType()) : ServiceType.DAILY)
                .mealType(req.mealType() != null ? MealType.valueOf(req.mealType()) : null)
                .serviceDate(req.serviceDate())
                .serviceTime(req.serviceTime())
                .guestsCount(guests)
                .numberOfMeals(meals)
                .menuId(req.menuId())
                .menuName(menuName)
                .specialRequests(req.specialRequests())
                .address(req.address())
                .city(req.city())
                .locality(req.locality())
                .pincode(req.pincode())
                .totalAmountPaise(totalAmountPaise)
                .advanceAmountPaise(advanceAmountPaise)
                .balanceAmountPaise(balanceAmountPaise)
                .platformFeePaise(platformFeePaise)
                .chefEarningsPaise(chefEarningsPaise)
                .paymentStatus("UNPAID")
                .status(ChefBookingStatus.PENDING_PAYMENT)
                .build();

        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking created: {} ref={} chef={} customer={}", saved.getId(), saved.getBookingRef(), chef.getId(), customerId);

        try {
            kafka.send("chef.booking.created", saved.getId().toString(),
                    buildEventJson(saved, chef));
        } catch (Exception e) {
            log.warn("Failed to send chef.booking.created Kafka event: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public ChefBooking confirmPayment(UUID customerId, UUID bookingId, String razorpayOrderId, String razorpayPaymentId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to confirm payment for this booking");
        }
        if (booking.getStatus() != ChefBookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Booking is not awaiting payment");
        }

        booking.setRazorpayOrderId(razorpayOrderId);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setPaymentStatus("ADVANCE_PAID");
        booking.setStatus(ChefBookingStatus.PENDING);

        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking advance payment confirmed: {} razorpayPaymentId={}", bookingId, razorpayPaymentId);

        try {
            kafka.send("chef.booking.payment.confirmed", saved.getId().toString(), buildEventJson(saved, null));
        } catch (Exception e) {
            log.warn("Kafka chef.booking.payment.confirmed failed: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public ChefBooking confirmBooking(UUID chefId, UUID bookingId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (!booking.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to confirm this booking");
        }
        if (booking.getStatus() != ChefBookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is not in PENDING status");
        }

        booking.setStatus(ChefBookingStatus.CONFIRMED);
        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking confirmed: {}", bookingId);
        try { kafka.send("chef.booking.confirmed", saved.getId().toString(), buildEventJson(saved, chef)); }
        catch (Exception e) { log.warn("Kafka chef.booking.confirmed failed: {}", e.getMessage()); }
        return saved;
    }

    @Transactional
    public ChefBooking cancelBooking(UUID userId, UUID bookingId, String reason) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Either the customer or the chef (by userId) can cancel
        ChefProfile chefProfile = chefProfileRepo.findByUserId(userId).orElse(null);
        boolean isChef = chefProfile != null && booking.getChefId().equals(chefProfile.getId());
        boolean isCustomer = booking.getCustomerId().equals(userId);

        if (!isChef && !isCustomer) {
            throw new IllegalArgumentException("Not authorized to cancel this booking");
        }
        if (booking.getStatus() == ChefBookingStatus.CANCELLED || booking.getStatus() == ChefBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Booking cannot be cancelled in current status");
        }

        // Determine refund amount: full advance refund for paid bookings
        boolean hasPaid = "ADVANCE_PAID".equals(booking.getPaymentStatus())
                || "PAID".equals(booking.getPaymentStatus());
        long refundPaise = hasPaid && booking.getAdvanceAmountPaise() != null
                ? booking.getAdvanceAmountPaise() : 0;

        booking.setStatus(ChefBookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(OffsetDateTime.now());
        if (refundPaise > 0) {
            booking.setPaymentStatus("REFUND_INITIATED");
        }
        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking cancelled: {} by userId={} refundPaise={}", bookingId, userId, refundPaise);

        // Send cancellation notification (with refund info)
        try {
            String eventJson = buildCancelEventJson(saved, reason, refundPaise);
            kafka.send("chef.booking.cancelled", saved.getId().toString(), eventJson);
        } catch (Exception e) {
            log.warn("Kafka chef.booking.cancelled failed: {}", e.getMessage());
        }

        // Request refund from payment-service via Kafka
        if (refundPaise > 0 && saved.getRazorpayPaymentId() != null) {
            try {
                String refundJson = String.format(
                        "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"razorpayPaymentId\":\"%s\","
                        + "\"amountPaise\":%d,\"reason\":\"%s\",\"refundType\":\"CHEF_BOOKING\","
                        + "\"customerId\":\"%s\",\"customerName\":\"%s\",\"chefName\":\"%s\"}",
                        saved.getId(), saved.getBookingRef(), saved.getRazorpayPaymentId(),
                        refundPaise, reason != null ? reason : "Booking cancelled",
                        saved.getCustomerId(),
                        saved.getCustomerName() != null ? saved.getCustomerName() : "",
                        saved.getChefName() != null ? saved.getChefName() : "");
                kafka.send("chef.booking.refund.requested", saved.getId().toString(), refundJson);
                log.info("Refund requested for chef booking {}: {} paise", saved.getBookingRef(), refundPaise);
            } catch (Exception e) {
                log.warn("Kafka chef.booking.refund.requested failed: {}", e.getMessage());
            }
        }

        return saved;
    }

    private String buildCancelEventJson(ChefBooking b, String reason, long refundPaise) {
        return String.format(
                "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"chefId\":\"%s\",\"customerId\":\"%s\","
                + "\"chefName\":\"%s\",\"customerName\":\"%s\",\"serviceDate\":\"%s\",\"mealType\":\"%s\","
                + "\"status\":\"%s\",\"totalAmountPaise\":%d,\"advanceAmountPaise\":%d,"
                + "\"balanceAmountPaise\":%d,\"paymentStatus\":\"%s\",\"city\":\"%s\","
                + "\"cancellationReason\":\"%s\",\"refundAmountPaise\":%d}",
                b.getId(), b.getBookingRef(), b.getChefId(), b.getCustomerId(),
                b.getChefName() != null ? b.getChefName() : "",
                b.getCustomerName() != null ? b.getCustomerName() : "",
                b.getServiceDate(), b.getMealType() != null ? b.getMealType() : "",
                b.getStatus(), b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0,
                b.getAdvanceAmountPaise() != null ? b.getAdvanceAmountPaise() : 0,
                b.getBalanceAmountPaise() != null ? b.getBalanceAmountPaise() : 0,
                b.getPaymentStatus() != null ? b.getPaymentStatus() : "UNPAID",
                b.getCity() != null ? b.getCity() : "",
                reason != null ? reason : "", refundPaise);
    }

    @Transactional
    public ChefBooking completeBooking(UUID chefId, UUID bookingId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (!booking.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized to complete this booking");
        }
        if (booking.getStatus() != ChefBookingStatus.CONFIRMED && booking.getStatus() != ChefBookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Booking must be CONFIRMED or IN_PROGRESS to complete");
        }

        booking.setStatus(ChefBookingStatus.COMPLETED);
        booking.setCompletedAt(OffsetDateTime.now());

        // Update chef total bookings
        chef.setTotalBookings(chef.getTotalBookings() + 1);
        chefProfileRepo.save(chef);

        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking completed: {}", bookingId);
        try { kafka.send("chef.booking.completed", saved.getId().toString(), buildEventJson(saved, chef)); }
        catch (Exception e) { log.warn("Kafka chef.booking.completed failed: {}", e.getMessage()); }
        return saved;
    }

    @Transactional
    public ChefBooking rateBooking(UUID customerId, UUID bookingId, int rating, String comment) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to rate this booking");
        }
        if (booking.getStatus() != ChefBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only rate completed bookings");
        }
        if (booking.getRatingGiven() != null) {
            throw new IllegalArgumentException("Booking already rated");
        }

        booking.setRatingGiven(rating);
        booking.setReviewComment(comment);

        // Update chef's running average rating
        ChefProfile chef = chefProfileRepo.findById(booking.getChefId())
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
        double currentTotal = chef.getRating() * chef.getReviewCount();
        int newCount = chef.getReviewCount() + 1;
        double newRating = (currentTotal + rating) / newCount;
        chef.setRating(Math.round(newRating * 10.0) / 10.0);
        chef.setReviewCount(newCount);
        chefProfileRepo.save(chef);

        log.info("Chef booking rated: {} rating={} chef={}", bookingId, rating, booking.getChefId());
        return bookingRepo.save(booking);
    }

    @Transactional
    public ChefBooking modifyBooking(UUID customerId, UUID bookingId, ModifyChefBookingRequest req) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to modify this booking");
        }
        if (booking.getStatus() != ChefBookingStatus.PENDING_PAYMENT && booking.getStatus() != ChefBookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking can only be modified in PENDING_PAYMENT or PENDING status");
        }

        // Apply non-null fields
        if (req.serviceDate() != null) booking.setServiceDate(req.serviceDate());
        if (req.serviceTime() != null) booking.setServiceTime(req.serviceTime());
        if (req.specialRequests() != null) booking.setSpecialRequests(req.specialRequests());
        if (req.address() != null) booking.setAddress(req.address());
        if (req.city() != null) booking.setCity(req.city());
        if (req.locality() != null) booking.setLocality(req.locality());
        if (req.pincode() != null) booking.setPincode(req.pincode());

        // If guests/meals/menu changed, recalculate pricing
        boolean recalculate = false;
        if (req.guestsCount() != null) { booking.setGuestsCount(req.guestsCount()); recalculate = true; }
        if (req.numberOfMeals() != null) { booking.setNumberOfMeals(req.numberOfMeals()); recalculate = true; }
        if (req.menuId() != null) { booking.setMenuId(req.menuId()); recalculate = true; }

        if (recalculate) {
            int guests = booking.getGuestsCount() != null ? booking.getGuestsCount() : 1;
            int meals = booking.getNumberOfMeals() != null ? booking.getNumberOfMeals() : 1;

            long totalAmountPaise;
            if (booking.getMenuId() != null) {
                ChefMenu menu = menuRepo.findById(booking.getMenuId())
                        .orElseThrow(() -> new IllegalArgumentException("Menu not found"));
                totalAmountPaise = menu.getPricePerPlatePaise() * guests * meals;
                booking.setMenuName(menu.getName());
            } else {
                ChefProfile chef = chefProfileRepo.findById(booking.getChefId())
                        .orElseThrow(() -> new IllegalArgumentException("Chef not found"));
                totalAmountPaise = chef.getDailyRatePaise() * guests * meals;
            }

            long platformFeePaise = totalAmountPaise * 15 / 100;
            long chefEarningsPaise = totalAmountPaise - platformFeePaise;
            long advanceAmountPaise = Math.max(totalAmountPaise * 10 / 100, 100);
            long balanceAmountPaise = totalAmountPaise - advanceAmountPaise;

            booking.setTotalAmountPaise(totalAmountPaise);
            booking.setPlatformFeePaise(platformFeePaise);
            booking.setChefEarningsPaise(chefEarningsPaise);
            booking.setAdvanceAmountPaise(advanceAmountPaise);
            booking.setBalanceAmountPaise(balanceAmountPaise);
        }

        booking.setModifiedAt(OffsetDateTime.now());
        booking.setModificationCount(booking.getModificationCount() != null ? booking.getModificationCount() + 1 : 1);

        ChefBooking saved = bookingRepo.save(booking);
        log.info("Chef booking modified: {} by customer={}", bookingId, customerId);

        try {
            kafka.send("chef.booking.modified", saved.getId().toString(), buildEventJson(saved, null));
        } catch (Exception e) {
            log.warn("Kafka chef.booking.modified failed: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ChefBooking> adminListAll(org.springframework.data.domain.Pageable pageable) {
        return bookingRepo.findAll(pageable);
    }

    @Transactional
    public ChefBooking adminCancelBooking(UUID bookingId, String reason) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getStatus() == ChefBookingStatus.CANCELLED || booking.getStatus() == ChefBookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Booking cannot be cancelled in current status");
        }

        boolean hasPaid = "ADVANCE_PAID".equals(booking.getPaymentStatus())
                || "PAID".equals(booking.getPaymentStatus());
        long refundPaise = hasPaid && booking.getAdvanceAmountPaise() != null
                ? booking.getAdvanceAmountPaise() : 0;

        booking.setStatus(ChefBookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : "Cancelled by admin");
        booking.setCancelledAt(OffsetDateTime.now());
        if (refundPaise > 0) {
            booking.setPaymentStatus("REFUND_INITIATED");
        }
        ChefBooking saved = bookingRepo.save(booking);
        log.info("Admin cancelled chef booking: {} refundPaise={}", bookingId, refundPaise);

        try {
            kafka.send("chef.booking.cancelled", saved.getId().toString(),
                    buildCancelEventJson(saved, saved.getCancellationReason(), refundPaise));
        } catch (Exception e) {
            log.warn("Kafka chef.booking.cancelled failed: {}", e.getMessage());
        }

        if (refundPaise > 0 && saved.getRazorpayPaymentId() != null) {
            try {
                String refundJson = String.format(
                        "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"razorpayPaymentId\":\"%s\","
                        + "\"amountPaise\":%d,\"reason\":\"%s\",\"refundType\":\"CHEF_BOOKING\","
                        + "\"customerId\":\"%s\",\"customerName\":\"%s\",\"chefName\":\"%s\"}",
                        saved.getId(), saved.getBookingRef(), saved.getRazorpayPaymentId(),
                        refundPaise, saved.getCancellationReason(),
                        saved.getCustomerId(),
                        saved.getCustomerName() != null ? saved.getCustomerName() : "",
                        saved.getChefName() != null ? saved.getChefName() : "");
                kafka.send("chef.booking.refund.requested", saved.getId().toString(), refundJson);
            } catch (Exception e) {
                log.warn("Kafka chef.booking.refund.requested failed: {}", e.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public ChefBooking adminCompleteBooking(UUID bookingId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getStatus() == ChefBookingStatus.COMPLETED || booking.getStatus() == ChefBookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking cannot be completed in current status");
        }
        booking.setStatus(ChefBookingStatus.COMPLETED);
        booking.setCompletedAt(OffsetDateTime.now());
        ChefBooking saved = bookingRepo.save(booking);
        log.info("Admin completed chef booking: {}", bookingId);
        try { kafka.send("chef.booking.completed", saved.getId().toString(), buildEventJson(saved, null)); }
        catch (Exception e) { log.warn("Kafka chef.booking.completed failed: {}", e.getMessage()); }
        return saved;
    }

    @Transactional
    public ChefBooking adminAssignChef(UUID bookingId, UUID chefId) {
        ChefBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        ChefProfile chef = chefProfileRepo.findById(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef not found"));

        booking.setChefId(chef.getId());
        booking.setChefName(chef.getName());
        log.info("Admin assigned chef {} to booking {}", chefId, bookingId);
        return bookingRepo.save(booking);
    }

    @Transactional(readOnly = true)
    public List<ChefBooking> getMyBookings(UUID customerId) {
        return bookingRepo.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<ChefBooking> getChefBookings(UUID chefId) {
        ChefProfile chef = chefProfileRepo.findByUserId(chefId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        return bookingRepo.findByChefId(chef.getId());
    }

    @Transactional
    public ChefBooking rebook(UUID customerId, UUID originalBookingId, java.time.LocalDate newDate, String newTime) {
        ChefBooking original = bookingRepo.findById(originalBookingId)
                .orElseThrow(() -> new IllegalArgumentException("Original booking not found"));

        if (!original.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Not authorized to rebook");
        }

        // Create a new booking from the original, same chef/menu/guests/address
        CreateChefBookingRequest req = new CreateChefBookingRequest(
                original.getChefId(),
                original.getServiceType() != null ? original.getServiceType().name() : "DAILY",
                original.getMealType() != null ? original.getMealType().name() : null,
                newDate,
                newTime != null ? newTime : original.getServiceTime(),
                original.getGuestsCount(),
                original.getNumberOfMeals(),
                original.getMenuId(),
                original.getSpecialRequests(),
                original.getAddress(),
                original.getCity(),
                original.getLocality(),
                original.getPincode(),
                original.getCustomerName(),
                null // phone not stored on booking
        );

        log.info("Rebooking from {} for customer={} newDate={}", originalBookingId, customerId, newDate);
        return createBooking(customerId, req);
    }

    @Transactional(readOnly = true)
    public ChefBooking getBooking(UUID bookingId) {
        return bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    private String buildEventJson(ChefBooking b, ChefProfile chef) {
        return String.format(
                "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"chefId\":\"%s\",\"customerId\":\"%s\","
                + "\"chefName\":\"%s\",\"customerName\":\"%s\",\"serviceDate\":\"%s\",\"mealType\":\"%s\","
                + "\"status\":\"%s\",\"totalAmountPaise\":%d,\"advanceAmountPaise\":%d,"
                + "\"balanceAmountPaise\":%d,\"paymentStatus\":\"%s\",\"city\":\"%s\"}",
                b.getId(), b.getBookingRef(), b.getChefId(), b.getCustomerId(),
                b.getChefName() != null ? b.getChefName() : (chef != null ? chef.getName() : ""),
                b.getCustomerName() != null ? b.getCustomerName() : "",
                b.getServiceDate(), b.getMealType() != null ? b.getMealType() : "",
                b.getStatus(), b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0,
                b.getAdvanceAmountPaise() != null ? b.getAdvanceAmountPaise() : 0,
                b.getBalanceAmountPaise() != null ? b.getBalanceAmountPaise() : 0,
                b.getPaymentStatus() != null ? b.getPaymentStatus() : "UNPAID",
                b.getCity() != null ? b.getCity() : "");
    }
}
