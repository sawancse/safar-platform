package com.safar.booking.service;

import com.safar.booking.dto.BookingGuestResponse;
import com.safar.booking.dto.BookingInclusionResponse;
import com.safar.booking.dto.BookingResponse;
import com.safar.booking.dto.BookingRoomSelectionResponse;
import com.safar.booking.dto.GroupBookingRequest;
import com.safar.booking.dto.GroupBookingResult;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingGuestRepository;
import com.safar.booking.repository.BookingInclusionRepository;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.BookingRoomSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBookingService {

    private final BookingRepository bookingRepo;
    private final BookingInclusionRepository inclusionRepo;
    private final BookingRoomSelectionRepository roomSelectionRepo;
    private final BookingGuestRepository guestRepo;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final ListingServiceClient listingClient;

    private static final long INSURANCE_PER_NIGHT_PAISE = 15000L; // Rs 150
    private static final long INSURANCE_CAP_PAISE = 150000L;       // Rs 1,500
    private static final double GST_RATE = 0.18;

    @Transactional
    public GroupBookingResult createGroupBooking(UUID guestId, GroupBookingRequest req) {
        if (req.listingIds() == null || req.listingIds().isEmpty()) {
            throw new IllegalArgumentException("At least one listing is required for a group booking");
        }

        long nights = ChronoUnit.DAYS.between(req.checkIn(), req.checkOut());
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        // Determine if bundle discount applies: all group listings must be booked
        List<UUID> allGroupListingIds = listingClient.getGroupListingIds(req.groupId());
        boolean allGroupListingsBooked = !allGroupListingIds.isEmpty()
                && new HashSet<>(req.listingIds()).containsAll(allGroupListingIds);
        int discountPct = allGroupListingsBooked ? listingClient.getGroupBundleDiscountPct(req.groupId()) : 0;

        UUID groupBookingId = UUID.randomUUID();
        List<UUID> bookingIds = new ArrayList<>();
        long totalAmountPaise = 0L;
        boolean first = true;

        for (UUID listingId : req.listingIds()) {
            if (!listingClient.isAvailable(listingId, req.checkIn(), req.checkOut())) {
                throw new IllegalArgumentException("Listing " + listingId + " is not available for selected dates");
            }

            // Hold dates in Redis for 15 minutes
            String holdKey = "booking:hold:" + listingId + ":" + req.checkIn().toLocalDate();
            redis.opsForValue().set(holdKey, guestId.toString(), Duration.ofMinutes(15));

            long basePaise = listingClient.getBasePricePaise(listingId) * nights;

            // Apply bundle discount to base price
            if (discountPct > 0) {
                basePaise = basePaise - (basePaise * discountPct / 100);
            }

            long insurancePaise = Math.min(INSURANCE_PER_NIGHT_PAISE * nights, INSURANCE_CAP_PAISE);
            long gstPaise = Math.round(basePaise * GST_RATE);
            long bookingTotal = basePaise + insurancePaise + gstPaise;

            Booking booking = Booking.builder()
                    .bookingRef(generateRef())
                    .guestId(guestId)
                    .hostId(listingClient.getHostId(listingId))
                    .listingId(listingId)
                    .checkIn(req.checkIn())
                    .checkOut(req.checkOut())
                    .guestsCount(req.guestsPerRoom())
                    .nights((int) nights)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .baseAmountPaise(basePaise)
                    .insuranceAmountPaise(insurancePaise)
                    .gstAmountPaise(gstPaise)
                    .totalAmountPaise(bookingTotal)
                    .hostPayoutPaise(basePaise)
                    .groupBookingId(groupBookingId)
                    .isPrimaryBooking(first)
                    .build();

            Booking saved = bookingRepo.save(booking);
            bookingIds.add(saved.getId());
            totalAmountPaise += bookingTotal;

            kafka.send("booking.created", saved.getId() != null ? saved.getId().toString() : "");
            log.info("Group booking: created booking {} for listing {}", saved.getBookingRef(), listingId);

            first = false;
        }

        log.info("Group booking {} created with {} bookings, discount={}%",
                groupBookingId, bookingIds.size(), discountPct);

        return new GroupBookingResult(groupBookingId, bookingIds, totalAmountPaise, discountPct > 0);
    }

    public List<BookingResponse> getGroupBookings(UUID groupBookingId) {
        return bookingRepo.findByGroupBookingId(groupBookingId).stream()
                .map(this::toResponse)
                .toList();
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

        return new BookingResponse(
                b.getId(), b.getBookingRef(),
                b.getGuestId(), b.getHostId(), b.getListingId(),
                b.getListingTitle(),
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
                b.getProcedureName(), b.getHospitalName(), b.getHospitalId(),
                b.getSpecialty(), b.getProcedureDate(),
                b.getHospitalDays(), b.getRecoveryDays(),
                b.getTreatmentCostPaise(), b.getPatientNotes(),
                b.getHostEarningsPaise(), b.getPlatformFeePaise(),
                b.getCleaningFeePaise(), b.getCommissionRate(),
                b.getHasReview(), b.getReviewRating(), b.getReviewedAt(),
                // PG/Hotel booking fields
                b.getNoticePeriodDays(), b.getSecurityDepositPaise(), b.getSecurityDepositStatus(),
                // Inclusions
                b.getInclusionsTotalPaise(), inclusions,
                // Room selections + guests
                roomSelectionRepo.findByBookingId(b.getId()).stream()
                        .map(rs -> new BookingRoomSelectionResponse(rs.getId(), rs.getRoomTypeId(),
                                rs.getRoomTypeName(), rs.getCount(), rs.getPricePerUnitPaise(), rs.getTotalPaise()))
                        .toList(),
                guestRepo.findByBookingId(b.getId()).stream()
                        .map(g -> new BookingGuestResponse(g.getId(), g.getFullName(), g.getEmail(),
                                g.getPhone(), g.getAge(), g.getIdType(), g.getIdNumber(),
                                g.getRoomAssignment(), g.getIsPrimary()))
                        .toList()
        );
    }
}
