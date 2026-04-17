package com.safar.booking.service;

import com.safar.booking.dto.BookingGuestResponse;
import com.safar.booking.dto.BookingInclusionResponse;
import com.safar.booking.dto.BookingResponse;
import com.safar.booking.dto.BookingRoomSelectionResponse;
import com.safar.booking.dto.CreateBookingRequest;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.BookingGuest;
import com.safar.booking.entity.BookingInclusion;
import com.safar.booking.entity.BookingRoomSelection;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.dto.CreateAgreementRequest;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingGuestRepository;
import com.safar.booking.repository.BookingInclusionRepository;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.BookingRoomSelectionRepository;
import com.safar.booking.util.CommissionRateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepo;
    private final BookingInclusionRepository inclusionRepo;
    private final BookingRoomSelectionRepository roomSelectionRepo;
    private final BookingGuestRepository guestRepo;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final ListingServiceClient listingClient;
    private final PgTenancyService pgTenancyService;
    private final TenancyAgreementService tenancyAgreementService;

    public ListingServiceClient getListingClient() { return listingClient; }

    private static final long INSURANCE_PER_NIGHT_PAISE = 15000L; // ₹150
    private static final long INSURANCE_CAP_PAISE = 150000L;       // ₹1,500
    private static final double GST_RATE = 0.18;

    @Transactional
    public BookingResponse createBooking(UUID guestId, CreateBookingRequest req) {
        // ── Step 1: Check listing availability (status + calendar dates) ──
        LocalDate checkInDate = req.checkIn().toLocalDate();
        LocalDate checkOutDate = req.checkOut().toLocalDate();
        Map<String, Object> availInfo = listingClient.checkAvailability(
                req.listingId(), checkInDate, checkOutDate);

        boolean available = Boolean.TRUE.equals(availInfo.get("available"));
        if (!available) {
            String listingStatus = (String) availInfo.get("status");
            String reason = "VERIFIED".equals(listingStatus)
                    ? "Some dates in the selected range are blocked"
                    : "Listing status is " + listingStatus + " (must be VERIFIED to accept bookings)";
            throw new IllegalArgumentException("Property not available: " + reason);
        }

        // ── Step 2: Room count ──
        int rooms = req.roomsCount() != null ? req.roomsCount() : 1;

        // ── Step 3: Room type validation (if selected) — takes priority over listing-level ──
        Integer maxGuestsPerRoom;
        Integer maxRoomsAvailable;

        if (req.roomSelections() != null && !req.roomSelections().isEmpty()) {
            // Multi-room: validate each room type's availability separately
            // count = total guests (rooms × guestsPerRoom for PG)
            // guests field or derive rooms from count and maxGuests
            for (var sel : req.roomSelections()) {
                Map<String, Object> rtAvail = listingClient.checkRoomTypeAvailability(
                        sel.roomTypeId(), checkInDate, checkOutDate);
                int minAvailable = rtAvail.get("minAvailable") instanceof Number
                        ? ((Number) rtAvail.get("minAvailable")).intValue() : 0;
                int rtMaxGuests = rtAvail.get("maxGuests") instanceof Number
                        ? ((Number) rtAvail.get("maxGuests")).intValue() : 1;
                // Derive actual rooms from count: for PG, count=guests, rooms = ceil(count/maxGuests)
                int actualRooms = rtMaxGuests > 0 ? (int) Math.ceil((double) sel.count() / rtMaxGuests) : sel.count();
                if (actualRooms > minAvailable) {
                    String rtName = listingClient.getRoomTypeName(req.listingId(), sel.roomTypeId());
                    throw new IllegalArgumentException(
                            "Only " + minAvailable + " rooms available for " + rtName + ", requested " + actualRooms);
                }
            }
            maxGuestsPerRoom = null; // per-type validation done above
            maxRoomsAvailable = null;
        } else if (req.roomTypeId() != null) {
            // Single room type: validate against available count
            Map<String, Object> rtAvail = listingClient.checkRoomTypeAvailability(
                    req.roomTypeId(), checkInDate, checkOutDate);

            int minAvailable = rtAvail.get("minAvailable") instanceof Number
                    ? ((Number) rtAvail.get("minAvailable")).intValue() : 0;

            maxGuestsPerRoom = rtAvail.get("maxGuests") instanceof Number
                    ? ((Number) rtAvail.get("maxGuests")).intValue() : null;
            maxRoomsAvailable = rtAvail.get("totalCount") instanceof Number
                    ? ((Number) rtAvail.get("totalCount")).intValue() : null;

            // Auto-scale room count from guest count when per-room cap is tight
            // (e.g. 5 guests on a PRIVATE room type with maxGuests=1 → need 5 rooms).
            if (maxGuestsPerRoom != null && maxGuestsPerRoom > 0) {
                int neededRooms = (int) Math.ceil((double) req.guestsCount() / maxGuestsPerRoom);
                if (neededRooms > rooms) {
                    log.info("Auto-scaling rooms from {} to {} for room type {} (guests={}, perRoom={})",
                            rooms, neededRooms, req.roomTypeId(), req.guestsCount(), maxGuestsPerRoom);
                    rooms = neededRooms;
                }
            }

            if (minAvailable < rooms) {
                throw new IllegalArgumentException(
                        "Only " + minAvailable + " rooms available for selected room type, requested " + rooms);
            }
        } else {
            // No room type selected — use listing-level limits
            maxGuestsPerRoom = availInfo.get("maxGuests") instanceof Number
                    ? ((Number) availInfo.get("maxGuests")).intValue() : null;
            maxRoomsAvailable = availInfo.get("totalRooms") instanceof Number
                    ? ((Number) availInfo.get("totalRooms")).intValue() : null;

            // Auto-scale room count from guest count against listing-level maxGuests
            if (maxGuestsPerRoom != null && maxGuestsPerRoom > 0) {
                int neededRooms = (int) Math.ceil((double) req.guestsCount() / maxGuestsPerRoom);
                if (neededRooms > rooms) {
                    log.info("Auto-scaling rooms from {} to {} for listing {} (guests={}, perRoom={})",
                            rooms, neededRooms, req.listingId(), req.guestsCount(), maxGuestsPerRoom);
                    rooms = neededRooms;
                }
            }
        }

        // ── Step 4: Validate guest count (skip for multi-room — already validated per type in step 3) ──
        if (maxGuestsPerRoom != null && (req.roomSelections() == null || req.roomSelections().isEmpty())
                && req.guestsCount() > maxGuestsPerRoom * rooms) {
            throw new IllegalArgumentException(
                    "Guest count " + req.guestsCount() + " exceeds maximum "
                    + (maxGuestsPerRoom * rooms) + " (" + maxGuestsPerRoom + " per room × " + rooms + " rooms)");
        }

        // ── Step 5: Validate room count against total (skip for multi-room — already validated per type) ──
        if (maxRoomsAvailable != null && (req.roomSelections() == null || req.roomSelections().isEmpty())
                && rooms > maxRoomsAvailable) {
            throw new IllegalArgumentException(
                    "Requested rooms " + rooms + " exceeds available " + maxRoomsAvailable);
        }

        // ── Step 6: Validate pets ──
        int pets = req.petsCount() != null ? req.petsCount() : 0;
        if (pets > 0) {
            Boolean petFriendly = availInfo.get("petFriendly") instanceof Boolean
                    ? (Boolean) availInfo.get("petFriendly") : false;
            if (!Boolean.TRUE.equals(petFriendly)) {
                throw new IllegalArgumentException("This property does not allow pets");
            }
            Integer maxPets = availInfo.get("maxPets") instanceof Number
                    ? ((Number) availInfo.get("maxPets")).intValue() : 0;
            if (maxPets > 0 && pets > maxPets) {
                throw new IllegalArgumentException(
                        "Pet count " + pets + " exceeds maximum " + maxPets + " for this property");
            }
        }

        // Hold dates in Redis with per-date keys for the full range
        try {
            for (LocalDate d = checkInDate; !d.isAfter(checkOutDate.minusDays(1)); d = d.plusDays(1)) {
                String holdKey = "booking:hold:" + req.listingId() + ":" + d;
                redis.opsForValue().set(holdKey, guestId.toString(), Duration.ofMinutes(15));
            }
        } catch (Exception e) {
            log.warn("Failed to set Redis hold for listing {}: {}", req.listingId(), e.getMessage());
        }

        // Determine listing type and pricing unit
        String listingType = listingClient.getListingType(req.listingId());
        String pricingUnit = listingClient.getPricingUnit(req.listingId());
        boolean isPgColiving = "PG".equals(listingType) || "COLIVING".equals(listingType);
        boolean isHotelType = "HOTEL".equals(listingType) || "BUDGET_HOTEL".equals(listingType)
                || "HOSTEL_DORM".equals(listingType);

        // Infer pricing unit from listing type if not explicitly set
        if ((pricingUnit == null || "NIGHT".equals(pricingUnit)) && isPgColiving) pricingUnit = "MONTH";
        if (pricingUnit == null) pricingUnit = "NIGHT";

        // Calculate duration
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        // PG/Monthly: require minimum 30 days
        if ("MONTH".equals(pricingUnit) && nights < 30) {
            throw new IllegalArgumentException("Monthly-priced bookings require a minimum stay of 30 days");
        }

        // Use room type price if roomTypeId is provided
        long baseRate; // the rate per pricingUnit (per night, per month, or per hour)
        String roomTypeName = null;
        if (req.roomTypeId() != null) {
            baseRate = listingClient.getRoomTypePrice(req.listingId(), req.roomTypeId());
            roomTypeName = listingClient.getRoomTypeName(req.listingId(), req.roomTypeId());
            if (baseRate <= 0) {
                baseRate = listingClient.getBasePricePaise(req.listingId());
            }
        } else {
            baseRate = listingClient.getBasePricePaise(req.listingId());
        }

        // ── Calculate base amount based on pricing unit ──
        long basePaise;

        // Multi-room-type pricing: sum each room type's price × count × duration
        if (req.roomSelections() != null && !req.roomSelections().isEmpty()) {
            basePaise = 0;
            for (var sel : req.roomSelections()) {
                long rtPrice = listingClient.getRoomTypePrice(req.listingId(), sel.roomTypeId());
                if (rtPrice <= 0) rtPrice = baseRate;
                switch (pricingUnit) {
                    case "MONTH" -> {
                        long fullMonths = nights / 30;
                        long remainingDays = nights % 30;
                        long monthlyTotal = rtPrice * fullMonths;
                        long proratedDays = remainingDays > 0
                                ? Math.round((double) rtPrice * remainingDays / 30) : 0;
                        basePaise += (monthlyTotal + proratedDays) * sel.count();
                    }
                    case "HOUR" -> {
                        long hours = req.hours() != null ? req.hours()
                                : ChronoUnit.HOURS.between(req.checkIn(), req.checkOut());
                        if (hours <= 0) hours = 1;
                        basePaise += rtPrice * hours * sel.count();
                    }
                    default -> basePaise += rtPrice * nights * sel.count();
                }
            }
            // Update rooms count to total of all selections
            rooms = req.roomSelections().stream().mapToInt(s -> s.count()).sum();
        } else {
            // Single room type or listing-level pricing
            switch (pricingUnit) {
                case "MONTH" -> {
                    long fullMonths = nights / 30;
                    long remainingDays = nights % 30;
                    long monthlyTotal = baseRate * fullMonths;
                    long proratedDays = remainingDays > 0
                            ? Math.round((double) baseRate * remainingDays / 30) : 0;
                    basePaise = (monthlyTotal + proratedDays) * rooms;
                }
                case "HOUR" -> {
                    long hours = req.hours() != null ? req.hours()
                            : ChronoUnit.HOURS.between(req.checkIn(), req.checkOut());
                    if (hours <= 0) hours = 1;
                    basePaise = baseRate * hours * rooms;
                }
                default -> basePaise = baseRate * nights * rooms;
            }
        }
        // PG/Co-living: charge only 1 month rent upfront (remaining months via monthly invoices)
        int leaseDurationMonths = 0;
        if (isPgColiving && "MONTH".equals(pricingUnit)) {
            leaseDurationMonths = (int) Math.max(1, nights / 30);
            // Override basePaise to 1 month only
            if (req.roomSelections() != null && !req.roomSelections().isEmpty()) {
                basePaise = 0;
                for (var sel : req.roomSelections()) {
                    long rtPrice = listingClient.getRoomTypePrice(req.listingId(), sel.roomTypeId());
                    if (rtPrice <= 0) rtPrice = baseRate;
                    basePaise += rtPrice * sel.count(); // 1 month per bed
                }
            } else {
                basePaise = baseRate * rooms; // 1 month rent only
            }
        }

        long cleaningFee = "MONTH".equals(pricingUnit) ? 0L : listingClient.getCleaningFeePaise(req.listingId());

        // PG/Co-living: fetch notice period and security deposit from listing
        Integer noticePeriodDays = null;
        Long securityDepositPaise = null;
        String securityDepositStatus = null;
        if (isPgColiving) {
            noticePeriodDays = req.noticePeriodDays() != null
                    ? req.noticePeriodDays() : listingClient.getNoticePeriodDays(req.listingId());
            Long listingDeposit = listingClient.getSecurityDepositPaise(req.listingId());

            if (req.securityDepositPaise() != null) {
                // Frontend sends pre-calculated total deposit
                securityDepositPaise = req.securityDepositPaise();
            } else if (req.roomSelections() != null && !req.roomSelections().isEmpty()) {
                // Multi-room: sum each room type's deposit × count
                long totalDeposit = 0;
                for (var sel : req.roomSelections()) {
                    Long rtDeposit = listingClient.getRoomTypeSecurityDepositPaise(sel.roomTypeId());
                    long perBed = (rtDeposit != null && rtDeposit > 0) ? rtDeposit
                            : (listingDeposit != null ? listingDeposit : 0L);
                    totalDeposit += perBed * sel.count();
                }
                securityDepositPaise = totalDeposit > 0 ? totalDeposit : null;
            } else if (req.roomTypeId() != null) {
                Long rtDeposit = listingClient.getRoomTypeSecurityDepositPaise(req.roomTypeId());
                long perBed = (rtDeposit != null && rtDeposit > 0) ? rtDeposit
                        : (listingDeposit != null ? listingDeposit : 0L);
                securityDepositPaise = perBed * rooms;
            } else {
                securityDepositPaise = listingDeposit != null ? listingDeposit * rooms : null;
            }
            securityDepositStatus = securityDepositPaise != null && securityDepositPaise > 0 ? "PENDING" : null;
        }
        // Insurance is a stay-protection fee for short-term bookings; skip for
        // residential monthly rentals (PG/co-living) — same as GST and cleaning fee.
        long insurancePaise = "MONTH".equals(pricingUnit)
                ? 0L
                : Math.min(INSURANCE_PER_NIGHT_PAISE * nights, INSURANCE_CAP_PAISE);

        // Feature 1: Non-refundable discount
        boolean isNonRefundable = Boolean.TRUE.equals(req.nonRefundable());
        long nonRefundableDiscountPaise = 0;
        if (isNonRefundable) {
            // Fetch discount percent from listing (default 10%)
            int discountPct = 10;
            try { discountPct = listingClient.getNonRefundableDiscountPercent(req.listingId()); }
            catch (Exception ignored) {}
            nonRefundableDiscountPaise = Math.round(basePaise * discountPct / 100.0);
            basePaise -= nonRefundableDiscountPaise;
        }

        // ── Inclusions pricing ──
        long inclusionsTotalPaise = 0L;
        List<Map<String, Object>> inclusionSnapshots = new ArrayList<>();
        if (req.selectedInclusionIds() != null && !req.selectedInclusionIds().isEmpty()
                && req.roomTypeId() != null) {
            List<Map<String, Object>> allInclusions = listingClient.getRoomTypeInclusions(req.roomTypeId());
            Set<String> selectedIds = req.selectedInclusionIds().stream()
                    .map(UUID::toString).collect(Collectors.toSet());

            for (Map<String, Object> inc : allInclusions) {
                String incId = inc.get("id") != null ? inc.get("id").toString() : "";
                if (!selectedIds.contains(incId)) continue;

                String mode = inc.get("inclusionMode") != null ? inc.get("inclusionMode").toString() : "INCLUDED";
                // Only PAID_ADDON items add to the total; INCLUDED and COMPLIMENTARY are free
                long chargePer = inc.get("chargePaise") instanceof Number
                        ? ((Number) inc.get("chargePaise")).longValue() : 0L;
                String chargeTypeStr = inc.get("chargeType") != null ? inc.get("chargeType").toString() : "PER_STAY";

                long itemTotal = 0L;
                if ("PAID_ADDON".equals(mode) && chargePer > 0) {
                    itemTotal = switch (chargeTypeStr) {
                        case "PER_NIGHT" -> chargePer * nights * rooms;
                        case "PER_PERSON" -> chargePer * req.guestsCount();
                        case "PER_HOUR" -> {
                            long hrs = req.hours() != null ? req.hours()
                                    : ChronoUnit.HOURS.between(req.checkIn(), req.checkOut());
                            yield chargePer * (hrs > 0 ? hrs : 1);
                        }
                        case "PER_USE" -> chargePer * rooms;
                        default -> chargePer; // PER_STAY
                    };
                }
                inclusionsTotalPaise += itemTotal;

                // Snapshot for saving after booking
                Map<String, Object> snap = new HashMap<>(inc);
                snap.put("totalPaise", itemTotal);
                snap.put("quantity", 1);
                inclusionSnapshots.add(snap);
            }
        }

        // PG/Co-living billing: price is per-bed, room count already represents beds/persons.
        // For multi-room selections: each selection's count = beds booked for that type.
        // For single room type: rooms count = beds booked.
        // NO separate adults multiplier — the count IS the number of adults.
        // Deposit: per-bed, already multiplied by room count in the selection loop or by rooms.

        // GST: exempt for residential monthly rent (Indian tax law)
        boolean isCommercialType = "COMMERCIAL".equals(listingType);
        long gstPaise = (isCommercialType || !"MONTH".equals(pricingUnit))
                ? Math.round(basePaise * GST_RATE) : 0L;
        // Deposit already computed as total (per-bed × beds) above for PG, or single amount otherwise
        long depositPaise = securityDepositPaise != null ? securityDepositPaise : 0L;
        long totalPaise = basePaise + cleaningFee + insurancePaise + gstPaise + inclusionsTotalPaise + depositPaise;

        // Feature 2: Pay at Property
        String paymentMode = req.paymentMode() != null ? req.paymentMode() : "PREPAID";
        Long prepaidAmountPaise = null;
        Long dueAtPropertyPaise = null;
        if ("PAY_AT_PROPERTY".equals(paymentMode)) {
            prepaidAmountPaise = 0L;
            dueAtPropertyPaise = totalPaise;
        } else if ("PARTIAL_PREPAID".equals(paymentMode)) {
            int prepaidPct = 30; // default 30% now
            try { prepaidPct = listingClient.getPartialPrepaidPercent(req.listingId()); }
            catch (Exception ignored) {}
            prepaidAmountPaise = Math.round(totalPaise * prepaidPct / 100.0);
            dueAtPropertyPaise = totalPaise - prepaidAmountPaise;
        }

        // Commission model
        UUID hostId2 = listingClient.getHostId(req.listingId());
        String hostTier = listingClient.getHostTier(hostId2);
        String bookingType;
        if (req.bookingType() != null) {
            bookingType = req.bookingType();
        } else if (isPgColiving) {
            bookingType = "PG";
        } else {
            bookingType = "SHORT_TERM";
        }
        BigDecimal commRate = CommissionRateUtil.getRate(hostTier, bookingType);
        long commPaise = BigDecimal.valueOf(basePaise).multiply(commRate)
                .setScale(0, RoundingMode.FLOOR).longValue();
        long hostEarnings = basePaise - commPaise + cleaningFee;

        Booking booking = Booking.builder()
                .bookingRef(generateRef())
                .guestId(guestId)
                .hostId(hostId2)
                .listingId(req.listingId())
                .listingTitle(listingClient.getListingTitle(req.listingId()))
                .listingCity(listingClient.getCity(req.listingId()))
                .listingType(listingType)
                .listingPhotoUrl(listingClient.getListingPhotoUrl(req.listingId()))
                .hostName(listingClient.getHostName(req.listingId()))
                .listingAddress(listingClient.getListingAddress(req.listingId()))
                .checkIn(req.checkIn())
                .checkOut(req.checkOut())
                .guestsCount(req.guestsCount())
                .adultsCount(req.adultsCount() != null ? req.adultsCount() : req.guestsCount())
                .childrenCount(req.childrenCount() != null ? req.childrenCount() : 0)
                .infantsCount(req.infantsCount() != null ? req.infantsCount() : 0)
                .petsCount(req.petsCount() != null ? req.petsCount() : 0)
                .roomsCount(req.roomsCount() != null ? req.roomsCount() : 1)
                .nights((int) nights)
                .status(BookingStatus.PENDING_PAYMENT)
                .baseAmountPaise(basePaise)
                .insuranceAmountPaise(insurancePaise)
                .gstAmountPaise(gstPaise)
                .totalAmountPaise(totalPaise)
                .hostPayoutPaise(hostEarnings)
                .cleaningFeePaise(cleaningFee)
                .hostEarningsPaise(hostEarnings)
                .platformFeePaise(commPaise)
                .commissionRate(commRate)
                .guestFirstName(req.guestFirstName())
                .guestLastName(req.guestLastName())
                .guestEmail(req.guestEmail())
                .guestPhone(req.guestPhone())
                .bookingFor(req.bookingFor() != null ? req.bookingFor() : "self")
                .travelForWork(req.travelForWork() != null ? req.travelForWork() : false)
                .airportShuttle(req.airportShuttle() != null ? req.airportShuttle() : false)
                .specialRequests(req.specialRequests())
                .arrivalTime(req.arrivalTime())
                .roomTypeId(req.roomTypeId())
                .roomTypeName(roomTypeName)
                .bookingType(bookingType)
                .organizationId("AASHRAY".equals(req.bookingType()) ? req.organizationId() : null)
                .caseWorkerId("AASHRAY".equals(req.bookingType()) ? req.caseWorkerId() : null)
                .monthlyRatePaise(req.monthlyRatePaise())
                .procedureName("MEDICAL".equals(req.bookingType()) ? req.procedureName() : null)
                .hospitalName("MEDICAL".equals(req.bookingType()) ? req.hospitalName() : null)
                .hospitalId("MEDICAL".equals(req.bookingType()) ? req.hospitalId() : null)
                .specialty("MEDICAL".equals(req.bookingType()) ? req.specialty() : null)
                .procedureDate("MEDICAL".equals(req.bookingType()) ? req.procedureDate() : null)
                .hospitalDays("MEDICAL".equals(req.bookingType()) ? req.hospitalDays() : null)
                .recoveryDays("MEDICAL".equals(req.bookingType()) ? req.recoveryDays() : null)
                .treatmentCostPaise("MEDICAL".equals(req.bookingType()) ? req.treatmentCostPaise() : null)
                .patientNotes("MEDICAL".equals(req.bookingType()) ? req.patientNotes() : null)
                .noticePeriodDays(noticePeriodDays)
                .securityDepositPaise(securityDepositPaise)
                .securityDepositStatus(securityDepositStatus)
                .nonRefundable(isNonRefundable)
                .nonRefundableDiscountPaise(nonRefundableDiscountPaise)
                .paymentMode(paymentMode)
                .prepaidAmountPaise(prepaidAmountPaise)
                .dueAtPropertyPaise(dueAtPropertyPaise)
                .inclusionsTotalPaise(inclusionsTotalPaise)
                .pricingUnit(pricingUnit)
                .leaseDurationMonths(isPgColiving ? leaseDurationMonths : null)
                .build();

        Booking saved = bookingRepo.save(booking);

        // Save booking inclusions (snapshots)
        if (!inclusionSnapshots.isEmpty()) {
            for (Map<String, Object> snap : inclusionSnapshots) {
                BookingInclusion bi = BookingInclusion.builder()
                        .bookingId(saved.getId())
                        .inclusionId(UUID.fromString(snap.get("id").toString()))
                        .category(snap.get("category") != null ? snap.get("category").toString() : "")
                        .name(snap.get("name") != null ? snap.get("name").toString() : "")
                        .description(snap.get("description") != null ? snap.get("description").toString() : null)
                        .inclusionMode(snap.get("inclusionMode") != null ? snap.get("inclusionMode").toString() : "INCLUDED")
                        .chargePaise(snap.get("chargePaise") instanceof Number
                                ? ((Number) snap.get("chargePaise")).longValue() : 0L)
                        .chargeType(snap.get("chargeType") != null ? snap.get("chargeType").toString() : "PER_STAY")
                        .discountPercent(snap.get("discountPercent") instanceof Number
                                ? ((Number) snap.get("discountPercent")).intValue() : 0)
                        .terms(snap.get("terms") != null ? snap.get("terms").toString() : null)
                        .quantity(1)
                        .totalPaise(snap.get("totalPaise") instanceof Number
                                ? ((Number) snap.get("totalPaise")).longValue() : 0L)
                        .build();
                inclusionRepo.save(bi);
            }
            log.info("Saved {} inclusions for booking {}", inclusionSnapshots.size(), saved.getBookingRef());
        }

        // Save multi-room-type selections
        if (req.roomSelections() != null && !req.roomSelections().isEmpty()) {
            for (var sel : req.roomSelections()) {
                long rtPrice = listingClient.getRoomTypePrice(req.listingId(), sel.roomTypeId());
                String rtName = listingClient.getRoomTypeName(req.listingId(), sel.roomTypeId());
                if (rtPrice <= 0) rtPrice = baseRate;
                long selTotal;
                switch (pricingUnit) {
                    case "MONTH" -> {
                        long fm = nights / 30;
                        long rd = nights % 30;
                        long mt = rtPrice * fm;
                        long pd = rd > 0 ? Math.round((double) rtPrice * rd / 30) : 0;
                        selTotal = (mt + pd) * sel.count();
                    }
                    case "HOUR" -> {
                        long hrs = req.hours() != null ? req.hours()
                                : ChronoUnit.HOURS.between(req.checkIn(), req.checkOut());
                        if (hrs <= 0) hrs = 1;
                        selTotal = rtPrice * hrs * sel.count();
                    }
                    default -> selTotal = rtPrice * sel.count() * nights;
                }
                roomSelectionRepo.save(BookingRoomSelection.builder()
                        .bookingId(saved.getId())
                        .roomTypeId(sel.roomTypeId())
                        .roomTypeName(rtName != null ? rtName : "Room")
                        .count(sel.count())
                        .pricePerUnitPaise(rtPrice)
                        .totalPaise(selTotal)
                        .build());
                // Decrement availability for each room type selection
                listingClient.decrementRoomTypeAvailability(
                        sel.roomTypeId(), checkInDate, checkOutDate, sel.count());
                // Reflect booking on host room board (occupiedBeds aggregate)
                listingClient.incrementRoomTypeOccupancy(sel.roomTypeId(), sel.count());
            }
            log.info("Saved {} room selections for booking {}", req.roomSelections().size(), saved.getBookingRef());
        }

        // Save guest list
        if (req.guests() != null && !req.guests().isEmpty()) {
            for (var g : req.guests()) {
                guestRepo.save(BookingGuest.builder()
                        .bookingId(saved.getId())
                        .fullName(g.fullName())
                        .email(g.email())
                        .phone(g.phone())
                        .age(g.age())
                        .idType(g.idType())
                        .idNumber(g.idNumber())
                        .roomAssignment(g.roomAssignment())
                        .isPrimary(g.isPrimary() != null ? g.isPrimary() : false)
                        .build());
            }
            log.info("Saved {} guests for booking {}", req.guests().size(), saved.getBookingRef());
        }

        // Decrement room type availability — HARD FAIL (single room type — legacy flow)
        if (req.roomTypeId() != null && (req.roomSelections() == null || req.roomSelections().isEmpty())) {
            listingClient.decrementRoomTypeAvailability(
                    req.roomTypeId(), checkInDate, checkOutDate, rooms);
            // Reflect booking on host room board (occupiedBeds aggregate)
            listingClient.incrementRoomTypeOccupancy(req.roomTypeId(), rooms);

            // For multi-room: only block listing calendar when ALL rooms of ALL types are booked
            Integer totalRooms = availInfo.get("totalRooms") instanceof Number
                    ? ((Number) availInfo.get("totalRooms")).intValue() : 1;
            if (totalRooms <= 1) {
                // Single-room: always block calendar
                listingClient.blockDates(req.listingId(), checkInDate, checkOutDate);
            }
            // Multi-room: calendar stays open — availability tracked via room-type inventory
        } else {
            // No room type selected: block calendar (single-room or legacy)
            listingClient.blockDates(req.listingId(), checkInDate, checkOutDate);
        }

        // PAY_AT_PROPERTY: auto-confirm (no online payment required)
        if ("PAY_AT_PROPERTY".equals(paymentMode)) {
            saved.setStatus(BookingStatus.CONFIRMED);
            saved = bookingRepo.save(saved);
            log.info("Auto-confirmed PAY_AT_PROPERTY booking: {}", saved.getBookingRef());
        }

        // Send Kafka event AFTER transaction commits so notification-service can find the booking
        final UUID savedId = saved.getId();
        final String savedRef = saved.getBookingRef();
        final boolean isConfirmed = saved.getStatus() == BookingStatus.CONFIRMED;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    kafka.send("booking.created", savedId != null ? savedId.toString() : "");
                    if (isConfirmed) {
                        kafka.send("booking.confirmed", savedId != null ? savedId.toString() : "");
                    }
                } catch (Exception e) {
                    log.warn("Failed to send Kafka event for created booking {}: {}", savedRef, e.getMessage());
                }
            }
        });

        // Publish medical-specific Kafka event
        if ("MEDICAL".equals(req.bookingType())) {
            try {
                String medicalEvent = String.format(
                        "{\"bookingId\":\"%s\",\"hospitalId\":\"%s\",\"procedureName\":\"%s\",\"specialty\":\"%s\",\"procedureDate\":\"%s\"}",
                        saved.getId(),
                        req.hospitalId() != null ? req.hospitalId() : "",
                        req.procedureName() != null ? req.procedureName() : "",
                        req.specialty() != null ? req.specialty() : "",
                        req.procedureDate() != null ? req.procedureDate() : ""
                );
                kafka.send("medical.booking.created", saved.getId().toString(), medicalEvent);
                log.info("Medical booking event published for booking {}", saved.getBookingRef());
            } catch (Exception e) {
                log.warn("Failed to send medical Kafka event for booking {}: {}", saved.getBookingRef(), e.getMessage());
            }
        }

        log.info("Booking {} created for listing {}", saved.getBookingRef(), req.listingId());
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only PENDING_PAYMENT bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmed = bookingRepo.save(booking);
        try {
            kafka.send("booking.confirmed", bookingId.toString());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for confirmed booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
        return toResponse(confirmed);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, String reason) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(userId) && !booking.getHostId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to cancel this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(OffsetDateTime.now());
        Booking cancelled = bookingRepo.save(booking);

        // Unblock dates on listing calendar
        try {
            listingClient.unblockDates(booking.getListingId(),
                    booking.getCheckIn().toLocalDate(), booking.getCheckOut().toLocalDate());
        } catch (Exception e) {
            log.warn("Failed to unblock dates for cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }

        // Increment room type availability on cancellation
        // Multi-room: release each room type separately
        List<BookingRoomSelection> roomSels = roomSelectionRepo.findByBookingId(bookingId);
        if (!roomSels.isEmpty()) {
            for (BookingRoomSelection sel : roomSels) {
                try {
                    listingClient.incrementRoomTypeAvailability(
                            sel.getRoomTypeId(),
                            booking.getCheckIn().toLocalDate(),
                            booking.getCheckOut().toLocalDate(),
                            sel.getCount());
                } catch (Exception e) {
                    log.warn("Failed to release room type {} for cancelled booking {}: {}",
                            sel.getRoomTypeName(), booking.getBookingRef(), e.getMessage());
                }
                try {
                    listingClient.decrementRoomTypeOccupancy(sel.getRoomTypeId(), sel.getCount());
                } catch (Exception e) {
                    log.warn("Failed to release room type occupancy for {}: {}", sel.getRoomTypeId(), e.getMessage());
                }
            }
        } else if (booking.getRoomTypeId() != null) {
            // Single room type (legacy flow)
            int rooms = booking.getRoomsCount() != null ? booking.getRoomsCount() : 1;
            try {
                listingClient.incrementRoomTypeAvailability(
                        booking.getRoomTypeId(),
                        booking.getCheckIn().toLocalDate(),
                        booking.getCheckOut().toLocalDate(),
                        rooms);
            } catch (Exception e) {
                log.warn("Failed to increment room type availability for cancelled booking {}: {}",
                        booking.getBookingRef(), e.getMessage());
            }
            try {
                listingClient.decrementRoomTypeOccupancy(booking.getRoomTypeId(), rooms);
            } catch (Exception e) {
                log.warn("Failed to release room type occupancy for {}: {}", booking.getRoomTypeId(), e.getMessage());
            }
        }

        // Release Redis hold
        try {
            String holdKey = "booking:hold:" + booking.getListingId()
                    + ":" + booking.getCheckIn().toLocalDate();
            redis.delete(holdKey);
        } catch (Exception e) {
            log.warn("Failed to release Redis hold for cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }

        try {
            kafka.send("booking.cancelled", bookingId.toString());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
        log.info("Booking {} cancelled by user {}", booking.getBookingRef(), userId);
        return toResponse(cancelled);
    }

    public BookingResponse getBooking(UUID bookingId) {
        return toResponse(getBookingById(bookingId));
    }

    public List<BookingResponse> getMyBookings(UUID guestId) {
        return bookingRepo.findByGuestId(guestId).stream()
                .map(this::toResponse).toList();
    }

    public List<BookingResponse> getHostBookings(UUID hostId) {
        return bookingRepo.findByHostId(hostId).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Applies wallet credits to a PENDING_PAYMENT booking, reducing its total.
     * Max applicable credits = booking total (100% coverage allowed).
     */
    @Transactional
    public BookingResponse applyWalletCredits(UUID bookingId, UUID guestId, long creditsToApply) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(guestId)) {
            throw new IllegalArgumentException("Booking does not belong to this guest");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Credits can only be applied to PENDING_PAYMENT bookings");
        }
        long maxApply = Math.min(creditsToApply, booking.getTotalAmountPaise());
        booking.setWalletCreditsAppliedPaise(booking.getWalletCreditsAppliedPaise() + maxApply);
        booking.setTotalAmountPaise(booking.getTotalAmountPaise() - maxApply);
        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public BookingResponse checkInBooking(UUID bookingId, UUID hostId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not have permission to manage this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be checked in");
        }
        // For PG bookings, block check-in upfront if the room is full so the host
        // gets a clear 409 on the UI instead of a CHECKED_IN booking with no tenancy.
        if ("PG".equals(booking.getBookingType()) && booking.getRoomTypeId() != null) {
            String sharingType = null;
            try {
                Map<String, Object> rtInfo = listingClient.getRoomTypeInfo(booking.getRoomTypeId());
                if (rtInfo != null && rtInfo.get("sharingType") != null) {
                    sharingType = rtInfo.get("sharingType").toString();
                }
            } catch (Exception ignored) {}
            pgTenancyService.assertBedsAvailable(booking.getRoomTypeId(), sharingType);
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(OffsetDateTime.now());
        Booking saved = bookingRepo.save(booking);
        try {
            kafka.send("booking.checked-in", bookingId.toString());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for checked-in booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
        log.info("Booking {} checked in by host {}", booking.getBookingRef(), hostId);

        // Auto-create PG tenancy on check-in for PG bookings
        if ("PG".equals(booking.getBookingType()) && booking.getRoomTypeId() != null) {
            try {
                // Determine sharing type from listing room type
                String sharingType = "TWO_SHARING"; // default for PG
                int bedsPerRoom = 2;
                try {
                    Map<String, Object> rtInfo = listingClient.getRoomTypeInfo(booking.getRoomTypeId());
                    if (rtInfo != null && rtInfo.get("sharingType") != null) {
                        sharingType = rtInfo.get("sharingType").toString();
                        bedsPerRoom = switch (sharingType) {
                            case "PRIVATE" -> 1;
                            case "TWO_SHARING" -> 2;
                            case "THREE_SHARING" -> 3;
                            case "FOUR_SHARING" -> 4;
                            case "DORMITORY" -> 6;
                            default -> 1;
                        };
                    }
                } catch (Exception ignored) {}

                // Find next available bed number
                String bedNumber = calculateNextAvailableBed(booking.getRoomTypeId(), bedsPerRoom);

                // Preflight bed check BEFORE opening the nested @Transactional in
                // PgTenancyService.createTenancy. Throwing from inside a nested TX
                // would mark this (outer) check-in TX rollback-only and blow up with
                // UnexpectedRollbackException even though we catch it below.
                if (booking.getRoomTypeId() != null) {
                    pgTenancyService.assertBedsAvailable(booking.getRoomTypeId(), sharingType);
                }

                PgTenancy createdTenancy = pgTenancyService.createTenancy(PgTenancy.builder()
                        .tenantId(booking.getGuestId())
                        .listingId(booking.getListingId())
                        .roomTypeId(booking.getRoomTypeId())
                        .bedNumber(bedNumber)
                        .sharingType(sharingType)
                        .moveInDate(booking.getCheckIn().toLocalDate())
                        .noticePeriodDays(booking.getNoticePeriodDays() != null ? booking.getNoticePeriodDays() : 30)
                        .monthlyRentPaise(booking.getBaseAmountPaise() > 0 ? booking.getBaseAmountPaise() : 0L)
                        .securityDepositPaise(booking.getSecurityDepositPaise() != null ? booking.getSecurityDepositPaise() : 0L)
                        .mealsIncluded(false)
                        .laundryIncluded(false)
                        .wifiIncluded(false)
                        .billingDay(1)
                        .build());
                log.info("Auto-created PG tenancy for booking {} with bed {}", booking.getBookingRef(), bedNumber);

                // Auto-create agreement and host-sign (check-in = implicit host consent)
                try {
                    String tenantName = (booking.getGuestFirstName() != null ? booking.getGuestFirstName() : "")
                            + " " + (booking.getGuestLastName() != null ? booking.getGuestLastName() : "");

                    // tenancy_agreements.host_name and property_address are NOT NULL; some
                    // booking rows don't carry them, so fall back to placeholders to avoid
                    // a constraint violation that silently kills the auto-agreement flow.
                    String hostName = booking.getHostName();
                    if (hostName == null || hostName.isBlank()) hostName = "Property Host";
                    String propertyAddress = booking.getListingAddress();
                    if (propertyAddress == null || propertyAddress.isBlank()) {
                        String title = booking.getListingTitle();
                        String city = booking.getListingCity();
                        propertyAddress = (title != null ? title : "Property")
                                + (city != null && !city.isBlank() ? ", " + city : "");
                    }

                    CreateAgreementRequest agreementReq = new CreateAgreementRequest(
                            tenantName.trim(),
                            booking.getGuestPhone(),
                            booking.getGuestEmail(),
                            null, // aadhaarLast4 — tenant adds later
                            hostName,
                            null, // hostPhone
                            propertyAddress,
                            sharingType + " - Bed " + bedNumber,
                            booking.getLeaseDurationMonths(), // lockInPeriodMonths
                            0L,   // maintenanceChargesPaise
                            null  // termsAndConditions — uses default template
                    );
                    tenancyAgreementService.createAgreement(createdTenancy.getId(), agreementReq);
                    tenancyAgreementService.hostSign(createdTenancy.getId(), hostId, "auto-checkin");
                    log.info("Auto-created and host-signed agreement for tenancy {}", createdTenancy.getTenancyRef());
                } catch (Exception ae) {
                    log.warn("Failed to auto-create agreement for tenancy {}: {}",
                            createdTenancy.getTenancyRef(), ae.getMessage());
                }
            } catch (Exception e) {
                log.warn("Failed to auto-create PG tenancy for booking {}: {}", booking.getBookingRef(), e.getMessage());
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public BookingResponse completeBooking(UUID bookingId, UUID hostId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not have permission to manage this booking");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Only CHECKED_IN bookings can be completed");
        }
        markBookingCompleted(booking);
        log.info("Booking {} completed by host {}", booking.getBookingRef(), hostId);
        return toResponse(booking);
    }

    /**
     * Shared transition: COMPLETED status + release occupiedBeds for each booked
     * room type so the room board reflects reality. Invoked from both the host
     * "mark complete" action and the scheduled auto-complete job.
     */
    private void markBookingCompleted(Booking booking) {
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(OffsetDateTime.now());
        bookingRepo.save(booking);

        // Release occupancy aggregates (room board / host dashboard)
        List<BookingRoomSelection> roomSels = roomSelectionRepo.findByBookingId(booking.getId());
        if (!roomSels.isEmpty()) {
            for (BookingRoomSelection sel : roomSels) {
                try {
                    listingClient.decrementRoomTypeOccupancy(sel.getRoomTypeId(), sel.getCount());
                } catch (Exception e) {
                    log.warn("Failed to release occupancy for completed booking {} (sel {}): {}",
                            booking.getBookingRef(), sel.getRoomTypeId(), e.getMessage());
                }
            }
        } else if (booking.getRoomTypeId() != null) {
            int rooms = booking.getRoomsCount() != null ? booking.getRoomsCount() : 1;
            try {
                listingClient.decrementRoomTypeOccupancy(booking.getRoomTypeId(), rooms);
            } catch (Exception e) {
                log.warn("Failed to release occupancy for completed booking {}: {}",
                        booking.getBookingRef(), e.getMessage());
            }
        }

        try {
            kafka.send("booking.completed", booking.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for completed booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }

    /**
     * Scheduled auto-complete: every hour, transition CONFIRMED/CHECKED_IN
     * bookings past their check-out timestamp into COMPLETED. Prevents host
     * room boards from showing stale "occupied" after checkout.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void autoCompletePastCheckOuts() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> candidates = bookingRepo.findByStatusInAndCheckOutBefore(
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN), now);
        if (candidates.isEmpty()) return;
        log.info("Auto-completing {} bookings past check-out", candidates.size());
        for (Booking b : candidates) {
            try {
                markBookingCompleted(b);
            } catch (Exception e) {
                log.warn("Auto-complete failed for booking {}: {}", b.getBookingRef(), e.getMessage());
            }
        }
    }

    @Transactional
    public BookingResponse refundDeposit(UUID bookingId, UUID hostId, String refundType,
                                          Long deductionPaise, String deductionReason) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        if (booking.getSecurityDepositPaise() == null || booking.getSecurityDepositPaise() <= 0) {
            throw new IllegalStateException("No deposit to refund");
        }
        if ("REFUNDED".equals(booking.getSecurityDepositStatus())) {
            throw new IllegalStateException("Deposit already refunded");
        }

        long depositAmount = booking.getSecurityDepositPaise();
        long refundAmount;
        if ("PARTIAL".equals(refundType) && deductionPaise != null && deductionPaise > 0) {
            refundAmount = Math.max(0, depositAmount - deductionPaise);
            booking.setSecurityDepositStatus("PARTIAL_REFUND");
        } else {
            refundAmount = depositAmount;
            booking.setSecurityDepositStatus("REFUNDED");
        }

        Booking saved = bookingRepo.save(booking);
        log.info("Deposit refund for booking {}: {} of {} (deduction: {} - {})",
                booking.getBookingRef(), refundAmount, depositAmount,
                deductionPaise != null ? deductionPaise : 0, deductionReason != null ? deductionReason : "none");
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse markNoShow(UUID bookingId, UUID hostId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not have permission to manage this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be marked as no-show");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        Booking saved = bookingRepo.save(booking);
        log.info("Booking {} marked no-show by host {}", booking.getBookingRef(), hostId);
        return toResponse(saved);
    }

    // ── Guest Management ─────────────────────────────────────────

    public List<BookingGuestResponse> getBookingGuests(UUID bookingId, UUID userId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(userId) && !booking.getHostId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to view guests for this booking");
        }
        return guestRepo.findByBookingId(bookingId).stream()
                .map(g -> new BookingGuestResponse(g.getId(), g.getFullName(), g.getEmail(),
                        g.getPhone(), g.getAge(), g.getIdType(), g.getIdNumber(),
                        g.getRoomAssignment(), g.getIsPrimary()))
                .toList();
    }

    @Transactional
    public BookingGuestResponse addBookingGuest(UUID bookingId, UUID userId, CreateBookingRequest.GuestInfo info) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(userId) && !booking.getHostId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to manage guests for this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify guests for a " + booking.getStatus() + " booking");
        }

        BookingGuest guest = BookingGuest.builder()
                .bookingId(bookingId)
                .fullName(info.fullName())
                .email(info.email())
                .phone(info.phone())
                .age(info.age())
                .idType(info.idType())
                .idNumber(info.idNumber())
                .roomAssignment(info.roomAssignment())
                .isPrimary(info.isPrimary() != null ? info.isPrimary() : false)
                .build();
        BookingGuest saved = guestRepo.save(guest);
        log.info("Added guest '{}' to booking {}", saved.getFullName(), booking.getBookingRef());
        return new BookingGuestResponse(saved.getId(), saved.getFullName(), saved.getEmail(),
                saved.getPhone(), saved.getAge(), saved.getIdType(), saved.getIdNumber(),
                saved.getRoomAssignment(), saved.getIsPrimary());
    }

    @Transactional
    public BookingGuestResponse updateBookingGuest(UUID bookingId, UUID guestId, UUID userId,
                                                    CreateBookingRequest.GuestInfo info) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(userId) && !booking.getHostId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to manage guests for this booking");
        }

        BookingGuest guest = guestRepo.findById(guestId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Guest not found: " + guestId));
        if (!guest.getBookingId().equals(bookingId)) {
            throw new IllegalArgumentException("Guest does not belong to this booking");
        }

        guest.setFullName(info.fullName());
        if (info.email() != null) guest.setEmail(info.email());
        if (info.phone() != null) guest.setPhone(info.phone());
        if (info.age() != null) guest.setAge(info.age());
        if (info.idType() != null) guest.setIdType(info.idType());
        if (info.idNumber() != null) guest.setIdNumber(info.idNumber());
        if (info.roomAssignment() != null) guest.setRoomAssignment(info.roomAssignment());
        if (info.isPrimary() != null) guest.setIsPrimary(info.isPrimary());

        BookingGuest saved = guestRepo.save(guest);
        log.info("Updated guest '{}' on booking {}", saved.getFullName(), booking.getBookingRef());
        return new BookingGuestResponse(saved.getId(), saved.getFullName(), saved.getEmail(),
                saved.getPhone(), saved.getAge(), saved.getIdType(), saved.getIdNumber(),
                saved.getRoomAssignment(), saved.getIsPrimary());
    }

    @Transactional
    public void removeBookingGuest(UUID bookingId, UUID guestId, UUID userId) {
        Booking booking = getBookingById(bookingId);
        if (!booking.getGuestId().equals(userId) && !booking.getHostId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to manage guests for this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify guests for a " + booking.getStatus() + " booking");
        }

        BookingGuest guest = guestRepo.findById(guestId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Guest not found: " + guestId));
        if (!guest.getBookingId().equals(bookingId)) {
            throw new IllegalArgumentException("Guest does not belong to this booking");
        }

        guestRepo.delete(guest);
        log.info("Removed guest '{}' from booking {}", guest.getFullName(), booking.getBookingRef());
    }

    /**
     * Calculate the next available bed number for a room type.
     * Bed numbers follow pattern: "1-A", "1-B", "2-A", "2-B", etc.
     * Room number increments, bed letter cycles A-F based on bedsPerRoom.
     */
    private String calculateNextAvailableBed(UUID roomTypeId, int bedsPerRoom) {
        // Get all active tenancies for this room type
        List<PgTenancy> activeTenancies = pgTenancyService.getActiveTenanciesByRoomType(roomTypeId);
        Set<String> occupiedBeds = activeTenancies.stream()
                .map(PgTenancy::getBedNumber)
                .filter(b -> b != null)
                .collect(java.util.stream.Collectors.toSet());

        // Generate bed labels: Room 1 → "1-A","1-B", Room 2 → "2-A","2-B", etc.
        char[] bedLetters = {'A', 'B', 'C', 'D', 'E', 'F'};
        for (int room = 1; room <= 100; room++) { // max 100 rooms
            for (int bed = 0; bed < bedsPerRoom && bed < bedLetters.length; bed++) {
                String bedId = room + "-" + bedLetters[bed];
                if (!occupiedBeds.contains(bedId)) {
                    return bedId;
                }
            }
        }
        // Fallback: all beds occupied, assign overflow
        return (occupiedBeds.size() + 1) + "-A";
    }

    private Booking getBookingById(UUID bookingId) {
        return bookingRepo.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
    }

    private String generateRef() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SAF-BKG-");
        new SecureRandom().ints(6, 0, chars.length())
                .forEach(i -> sb.append(chars.charAt(i)));
        return sb.toString();
    }

    private BookingResponse toResponse(Booking b) {
        List<BookingInclusionResponse> inclusions = inclusionRepo.findByBookingId(b.getId()).stream()
                .map(i -> new BookingInclusionResponse(
                        i.getId(), i.getInclusionId(), i.getCategory(), i.getName(),
                        i.getDescription(), i.getInclusionMode(), i.getChargePaise(),
                        i.getChargeType(), i.getDiscountPercent(), i.getTerms(),
                        i.getQuantity(), i.getTotalPaise()))
                .toList();

        List<BookingRoomSelectionResponse> roomSelections = roomSelectionRepo.findByBookingId(b.getId()).stream()
                .map(rs -> new BookingRoomSelectionResponse(
                        rs.getId(), rs.getRoomTypeId(), rs.getRoomTypeName(),
                        rs.getCount(), rs.getPricePerUnitPaise(), rs.getTotalPaise()))
                .toList();

        List<BookingGuestResponse> guests = guestRepo.findByBookingId(b.getId()).stream()
                .map(g -> new BookingGuestResponse(
                        g.getId(), g.getFullName(), g.getEmail(), g.getPhone(),
                        g.getAge(), g.getIdType(), g.getIdNumber(),
                        g.getRoomAssignment(), g.getIsPrimary()))
                .toList();

        return new BookingResponse(
                b.getId(), b.getBookingRef(),
                b.getGuestId(), b.getHostId(), b.getListingId(),
                b.getListingTitle(),
                b.getListingPhotoUrl(), b.getListingCity(), b.getListingType(),
                b.getCheckIn(), b.getCheckOut(), b.getGuestsCount(),
                b.getAdultsCount(), b.getChildrenCount(), b.getInfantsCount(), b.getPetsCount(),
                b.getRoomsCount(),
                b.getNights(),
                b.getStatus(),
                b.getBaseAmountPaise(), b.getInsuranceAmountPaise(),
                b.getGstAmountPaise(), b.getTotalAmountPaise(), b.getHostPayoutPaise(),
                b.getGuestFirstName(), b.getGuestLastName(),
                b.getGuestEmail(), b.getGuestPhone(),
                b.getBookingFor(), b.getTravelForWork(),
                b.getAirportShuttle(), b.getSpecialRequests(),
                b.getArrivalTime(),
                b.getCancellationReason(),
                b.getCreatedAt(), b.getUpdatedAt(),
                b.getCheckedInAt(), b.getCompletedAt(),
                b.getBookingType(), b.getOrganizationId(),
                b.getCaseWorkerId(), b.getMonthlyRatePaise(),
                b.getRoomTypeId(), b.getRoomTypeName(),
                b.getProcedureName(), b.getHospitalName(),
                b.getHospitalId(), b.getSpecialty(),
                b.getProcedureDate(), b.getHospitalDays(),
                b.getRecoveryDays(), b.getTreatmentCostPaise(),
                b.getPatientNotes(),
                b.getHostEarningsPaise(), b.getPlatformFeePaise(),
                b.getCleaningFeePaise(), b.getCommissionRate(),
                b.getHasReview(), b.getReviewRating(), b.getReviewedAt(),
                // PG/Hotel booking fields
                b.getNoticePeriodDays(), b.getSecurityDepositPaise(), b.getSecurityDepositStatus(),
                // Inclusions
                b.getInclusionsTotalPaise(), inclusions,
                // Room selections + guests
                roomSelections, guests,
                // Pricing unit
                b.getPricingUnit(),
                // Payment mode
                b.getPaymentMode()
        );
    }

    // ── Host booking search with filtering, sorting, pagination ─────────────

    public Page<BookingResponse> searchHostBookings(
            UUID hostId, String status, UUID listingId,
            LocalDate dateFrom, LocalDate dateTo, String search,
            String sortBy, String sortDir, int page, int size) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = switch (sortBy) {
            case "createdAt" -> "createdAt";
            case "amount" -> "totalAmountPaise";
            default -> "checkIn";
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<Booking> spec = Specification.where(hasHostId(hostId));

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), BookingStatus.valueOf(status)));
        }
        if (listingId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listingId"), listingId));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkIn"), dateFrom.atStartOfDay()));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkIn"), dateTo.plusDays(1).atStartOfDay()));
        }
        if (search != null && !search.isBlank()) {
            String q = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("guestFirstName")), q),
                    cb.like(cb.lower(root.get("guestLastName")), q),
                    cb.like(cb.lower(root.get("bookingRef")), q)
            ));
        }

        return bookingRepo.findAll(spec, pageable).map(this::toResponse);
    }

    private static Specification<Booking> hasHostId(UUID hostId) {
        return (root, query, cb) -> cb.equal(root.get("hostId"), hostId);
    }

    // ── Admin: search all bookings (no mandatory hostId) ────────────────────

    public Page<BookingResponse> searchAllBookings(
            String status, UUID hostId, UUID guestId, UUID listingId,
            LocalDate dateFrom, LocalDate dateTo, String search,
            String sortBy, String sortDir, int page, int size) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = switch (sortBy) {
            case "checkIn" -> "checkIn";
            case "amount" -> "totalAmountPaise";
            default -> "createdAt";
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<Booking> spec = Specification.where(null);

        if (hostId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("hostId"), hostId));
        }
        if (guestId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("guestId"), guestId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), BookingStatus.valueOf(status)));
        }
        if (listingId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("listingId"), listingId));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("checkIn"), dateFrom.atStartOfDay()));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("checkIn"), dateTo.plusDays(1).atStartOfDay()));
        }
        if (search != null && !search.isBlank()) {
            String q = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("guestFirstName")), q),
                    cb.like(cb.lower(root.get("guestLastName")), q),
                    cb.like(cb.lower(root.get("bookingRef")), q),
                    cb.like(cb.lower(root.get("listingTitle")), q)
            ));
        }

        return bookingRepo.findAll(spec, pageable).map(this::toResponse);
    }

    // ── Admin: bookings by host/guest ────────────────────────────────────────

    public List<BookingResponse> getBookingsByHostForAdmin(UUID hostId) {
        return bookingRepo.findByHostId(hostId).stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getBookingsByGuestForAdmin(UUID guestId) {
        return bookingRepo.findByGuestId(guestId).stream().map(this::toResponse).toList();
    }

    // ── Admin: cancel booking (bypasses ownership check) ─────────────────────

    @Transactional
    public BookingResponse adminCancelBooking(UUID bookingId, String reason) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("Admin: " + (reason != null ? reason : "Cancelled by admin"));
        booking.setCancelledAt(OffsetDateTime.now());
        Booking cancelled = bookingRepo.save(booking);

        try {
            listingClient.unblockDates(booking.getListingId(),
                    booking.getCheckIn().toLocalDate(), booking.getCheckOut().toLocalDate());
        } catch (Exception e) {
            log.warn("Failed to unblock dates for admin-cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }

        try {
            String holdKey = "booking:hold:" + booking.getListingId()
                    + ":" + booking.getCheckIn().toLocalDate();
            redis.delete(holdKey);
        } catch (Exception e) {
            log.warn("Failed to release Redis hold for admin-cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }

        try {
            kafka.send("booking.cancelled", bookingId.toString());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for admin-cancelled booking {}: {}", booking.getBookingRef(), e.getMessage());
        }

        log.info("Admin cancelled booking {} (ref: {})", bookingId, booking.getBookingRef());
        return toResponse(cancelled);
    }

    // ── Admin: confirm cash/COD booking ─────────────────────────────────────

    @Transactional
    public BookingResponse adminConfirmCashBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking cannot be confirmed in status: " + booking.getStatus());
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return toResponse(booking); // already confirmed
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmed = bookingRepo.save(booking);
        try { kafka.send("booking.confirmed", bookingId.toString()); }
        catch (Exception e) { log.warn("Kafka booking.confirmed failed: {}", e.getMessage()); }
        log.info("Admin confirmed cash booking {} (ref: {})", bookingId, booking.getBookingRef());
        return toResponse(confirmed);
    }

    @Transactional
    public BookingResponse adminRecordCashPayment(UUID bookingId, long amountPaise, String note) {
        Booking booking = getBookingById(bookingId);
        if (!"PAY_AT_PROPERTY".equals(booking.getPaymentMode()) && !"PARTIAL_PREPAID".equals(booking.getPaymentMode())) {
            throw new IllegalStateException("Cash payment recording only for PAY_AT_PROPERTY/PARTIAL_PREPAID bookings");
        }
        // Track cash collected
        long previousPaid = booking.getCashCollectedPaise() != null ? booking.getCashCollectedPaise() : 0;
        booking.setCashCollectedPaise(previousPaid + amountPaise);
        booking.setCashCollectionNote(note);

        // If fully paid, update payment mode status
        long totalDue = booking.getTotalAmountPaise() != null ? booking.getTotalAmountPaise() : 0;
        if (booking.getCashCollectedPaise() >= totalDue) {
            booking.setPaymentMode("CASH_COLLECTED");
        }

        Booking saved = bookingRepo.save(booking);
        log.info("Admin recorded cash payment {} paise for booking {} (total collected: {})",
                amountPaise, booking.getBookingRef(), booking.getCashCollectedPaise());
        return toResponse(saved);
    }

    // ── Admin: deposit refund (bypasses host ownership check) ───────────────

    @Transactional
    public BookingResponse adminRefundDeposit(UUID bookingId, String refundType,
                                               Long deductionPaise, String deductionReason) {
        Booking booking = getBookingById(bookingId);
        if (booking.getSecurityDepositPaise() == null || booking.getSecurityDepositPaise() <= 0) {
            throw new IllegalStateException("No deposit to refund");
        }
        if ("REFUNDED".equals(booking.getSecurityDepositStatus())) {
            throw new IllegalStateException("Deposit already refunded");
        }

        long depositAmount = booking.getSecurityDepositPaise();
        long refundAmount;
        if ("PARTIAL".equals(refundType) && deductionPaise != null && deductionPaise > 0) {
            refundAmount = Math.max(0, depositAmount - deductionPaise);
            booking.setSecurityDepositStatus("PARTIAL_REFUND");
        } else {
            refundAmount = depositAmount;
            booking.setSecurityDepositStatus("REFUNDED");
        }

        Booking saved = bookingRepo.save(booking);
        log.info("Admin deposit refund for booking {}: {} of {} (deduction: {} - {})",
                booking.getBookingRef(), refundAmount, depositAmount,
                deductionPaise != null ? deductionPaise : 0,
                deductionReason != null ? deductionReason : "none");

        try {
            kafka.send("deposit.refunded", Map.of(
                    "bookingId", bookingId.toString(),
                    "bookingRef", booking.getBookingRef(),
                    "guestId", booking.getGuestId().toString(),
                    "depositPaise", String.valueOf(depositAmount),
                    "refundPaise", String.valueOf(refundAmount),
                    "deductionPaise", String.valueOf(deductionPaise != null ? deductionPaise : 0),
                    "reason", deductionReason != null ? deductionReason : ""
            ).toString());
        } catch (Exception e) {
            log.warn("Failed to send deposit.refunded Kafka event: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    // ── Admin: list bookings with pending deposits ───────────���──────────────

    public Page<BookingResponse> getBookingsWithPendingDeposits(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return bookingRepo.findBySecurityDepositStatusIn(
                List.of("PENDING", "COLLECTED"), pageable).map(this::toResponse);
    }

    // ── Host calendar ───────────────────────────────────────────────────────

    public List<CalendarEntry> getHostCalendar(UUID hostId, LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepo.findByHostIdAndCheckInBetween(
                hostId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return bookings.stream()
                .map(b -> new CalendarEntry(
                        b.getId(), b.getListingId(), b.getBookingRef(),
                        b.getGuestFirstName() + " " + b.getGuestLastName(),
                        b.getCheckIn(), b.getCheckOut(),
                        b.getStatus().name(), b.getTotalAmountPaise()))
                .toList();
    }

    public record CalendarEntry(UUID bookingId, UUID listingId, String bookingRef,
                                String guestName, LocalDateTime checkIn, LocalDateTime checkOut,
                                String status, Long amountPaise) {}

    // ── Host Analytics ──────────────────────────────────────────────────────

    private static final List<BookingStatus> REVENUE_STATUSES = List.of(
            BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

    public Map<String, Object> getHostAnalytics(UUID hostId, int days) {
        List<Booking> allBookings = bookingRepo.findByHostId(hostId);

        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
        List<Booking> recentBookings = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutoff))
                .toList();

        // Total revenue
        long totalRevenuePaise = allBookings.stream()
                .filter(b -> REVENUE_STATUSES.contains(b.getStatus()))
                .mapToLong(b -> b.getHostEarningsPaise() != null ? b.getHostEarningsPaise()
                        : b.getHostPayoutPaise() != null ? b.getHostPayoutPaise() : b.getBaseAmountPaise())
                .sum();

        // Recent revenue
        long recentRevenuePaise = recentBookings.stream()
                .filter(b -> REVENUE_STATUSES.contains(b.getStatus()))
                .mapToLong(b -> b.getHostEarningsPaise() != null ? b.getHostEarningsPaise()
                        : b.getHostPayoutPaise() != null ? b.getHostPayoutPaise() : b.getBaseAmountPaise())
                .sum();

        // Booking counts by status
        Map<String, Long> statusCounts = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        // Total booked nights
        long totalNights = allBookings.stream()
                .filter(b -> REVENUE_STATUSES.contains(b.getStatus()))
                .mapToLong(b -> Math.max(1, Duration.between(b.getCheckIn(), b.getCheckOut()).toDays()))
                .sum();

        // Avg nightly rate
        long avgNightlyRate = totalNights > 0 ? totalRevenuePaise / totalNights : 0;

        // Cancellation rate
        long totalBookingCount = allBookings.size();
        long cancelledBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        double cancellationRate = totalBookingCount > 0
                ? (double) cancelledBookings / totalBookingCount * 100 : 0;

        // Bookings per listing
        Map<String, Long> perListing = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getListingId().toString(), Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenuePaise", totalRevenuePaise);
        result.put("recentRevenuePaise", recentRevenuePaise);
        result.put("totalBookings", totalBookingCount);
        result.put("totalNights", totalNights);
        result.put("avgNightlyRatePaise", avgNightlyRate);
        result.put("cancellationRate", Math.round(cancellationRate * 10) / 10.0);
        result.put("statusCounts", statusCounts);
        result.put("bookingsPerListing", perListing);
        result.put("periodDays", days);

        return result;
    }
}
