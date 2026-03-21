package com.safar.booking.service;

import com.safar.booking.dto.BookingResponse;
import com.safar.booking.dto.CreateBookingRequest;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingGuestRepository;
import com.safar.booking.repository.BookingInclusionRepository;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.BookingRoomSelectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock BookingRepository bookingRepo;
    @Mock BookingInclusionRepository inclusionRepo;
    @Mock BookingRoomSelectionRepository roomSelectionRepo;
    @Mock BookingGuestRepository guestRepo;
    @Mock StringRedisTemplate redis;
    @Mock KafkaTemplate<String, String> kafka;
    @Mock ListingServiceClient listingClient;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks BookingService bookingService;

    private final UUID guestId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(listingClient.getListingType(any())).thenReturn("HOME");
        when(inclusionRepo.findByBookingId(any())).thenReturn(java.util.List.of());
        when(roomSelectionRepo.findByBookingId(any())).thenReturn(java.util.List.of());
        when(guestRepo.findByBookingId(any())).thenReturn(java.util.List.of());
    }

    private Map<String, Object> availableResponse() {
        return Map.of("available", true, "maxGuests", 10, "totalRooms", 5,
                "petFriendly", false, "maxPets", 0);
    }

    private Map<String, Object> unavailableResponse() {
        return Map.of("available", false, "maxGuests", 10, "totalRooms", 5,
                "petFriendly", false, "maxPets", 0);
    }

    private CreateBookingRequest request(LocalDateTime in, LocalDateTime out) {
        return new CreateBookingRequest(listingId, in, out, 2,
                null, null, null, null,
                1, // roomsCount
                "John", "Doe", "john@example.com", "9876543210",
                "self", false, false, null,
                null, // arrivalTime
                null, // roomTypeId
                null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                // PG/Hotel booking fields
                null, null,
                // Hourly bookings
                null,
                // Non-refundable & Pay-at-Property
                null, null,
                // Inclusions
                null,
                // Room selections + guests
                null, null);
    }

    @Test
    void createBooking_availableProperty_createsPendingPayment() {
        LocalDateTime checkIn  = LocalDateTime.of(2025, 12, 20, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2025, 12, 25, 14, 0);

        when(listingClient.checkAvailability(eq(listingId), any(), any())).thenReturn(availableResponse());
        when(listingClient.getBasePricePaise(listingId)).thenReturn(500000L);
        when(listingClient.getHostId(listingId)).thenReturn(hostId);
        when(listingClient.getListingType(listingId)).thenReturn("HOME");
        when(listingClient.getPricingUnit(listingId)).thenReturn("NIGHT");
        when(listingClient.getHostTier(hostId)).thenReturn("STARTER");
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(guestId, request(checkIn, checkOut));

        assertThat(resp.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(resp.guestId()).isEqualTo(guestId);
        assertThat(resp.nights()).isEqualTo(5);
        verify(kafka).send(eq("booking.created"), anyString());
    }

    @Test
    void createBooking_unavailableProperty_throws() {
        LocalDateTime checkIn  = LocalDateTime.of(2025, 12, 20, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2025, 12, 25, 14, 0);

        when(listingClient.checkAvailability(eq(listingId), any(), any())).thenReturn(unavailableResponse());

        assertThatThrownBy(() -> bookingService.createBooking(guestId, request(checkIn, checkOut)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createBooking_calculatesInsuranceCapped() {
        // 15 nights would be 15 * ₹150 = ₹2,250 but capped at ₹1,500
        LocalDateTime checkIn  = LocalDateTime.of(2025, 12, 1, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2025, 12, 16, 11, 0); // 15 nights

        when(listingClient.checkAvailability(eq(listingId), any(), any())).thenReturn(availableResponse());
        when(listingClient.getBasePricePaise(listingId)).thenReturn(500000L);
        when(listingClient.getHostId(listingId)).thenReturn(hostId);
        when(listingClient.getListingType(listingId)).thenReturn("HOME");
        when(listingClient.getPricingUnit(listingId)).thenReturn("NIGHT");
        when(listingClient.getHostTier(hostId)).thenReturn("STARTER");
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(guestId, request(checkIn, checkOut));

        assertThat(resp.insuranceAmountPaise()).isEqualTo(150000L); // ₹1,500 cap
    }

    @Test
    void createBooking_calculatesInsuranceOneNight() {
        LocalDateTime checkIn  = LocalDateTime.of(2025, 12, 20, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2025, 12, 21, 14, 0);

        when(listingClient.checkAvailability(eq(listingId), any(), any())).thenReturn(availableResponse());
        when(listingClient.getBasePricePaise(listingId)).thenReturn(500000L);
        when(listingClient.getHostId(listingId)).thenReturn(hostId);
        when(listingClient.getListingType(listingId)).thenReturn("HOME");
        when(listingClient.getPricingUnit(listingId)).thenReturn("NIGHT");
        when(listingClient.getHostTier(hostId)).thenReturn("STARTER");
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(guestId, request(checkIn, checkOut));

        assertThat(resp.insuranceAmountPaise()).isEqualTo(15000L); // ₹150
    }

    @Test
    void createBooking_calculatesGstAt18Percent() {
        LocalDateTime checkIn  = LocalDateTime.of(2025, 12, 20, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2025, 12, 22, 14, 0); // 2 nights

        when(listingClient.checkAvailability(eq(listingId), any(), any())).thenReturn(availableResponse());
        when(listingClient.getBasePricePaise(listingId)).thenReturn(500000L);
        when(listingClient.getHostId(listingId)).thenReturn(hostId);
        when(listingClient.getListingType(listingId)).thenReturn("HOME");
        when(listingClient.getPricingUnit(listingId)).thenReturn("NIGHT");
        when(listingClient.getHostTier(hostId)).thenReturn("STARTER");
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(guestId, request(checkIn, checkOut));

        long expectedBase = 1000000L; // 2 * 500,000
        long expectedGst = Math.round(expectedBase * 0.18); // 180,000
        assertThat(resp.baseAmountPaise()).isEqualTo(expectedBase);
        assertThat(resp.gstAmountPaise()).isEqualTo(expectedGst);
    }

    @Test
    void confirmBooking_pendingPayment_transitionsToConfirmed() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-TEST1")
                .status(BookingStatus.PENDING_PAYMENT)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.confirmBooking(bookingId);

        assertThat(resp.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(kafka).send(eq("booking.confirmed"), eq(bookingId.toString()));
    }

    @Test
    void cancelBooking_confirmed_transitionsToCancelled() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-TEST2")
                .status(BookingStatus.CONFIRMED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.cancelBooking(bookingId, guestId, "Change of plans");

        assertThat(resp.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(resp.cancellationReason()).isEqualTo("Change of plans");
        verify(redis).delete(anyString());
        verify(kafka).send(eq("booking.cancelled"), eq(bookingId.toString()));
    }

    @Test
    void checkInBooking_confirmed_transitionsToCheckedIn() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-CI01")
                .status(BookingStatus.CONFIRMED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.checkInBooking(bookingId, hostId);

        assertThat(resp.status()).isEqualTo(BookingStatus.CHECKED_IN);
        verify(kafka).send(eq("booking.checked-in"), eq(bookingId.toString()));
    }

    @Test
    void checkInBooking_wrongHost_throws() {
        UUID bookingId = UUID.randomUUID();
        UUID wrongHost = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-CI02")
                .status(BookingStatus.CONFIRMED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkInBooking(bookingId, wrongHost))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not have permission");
    }

    @Test
    void checkInBooking_notConfirmed_throws() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-CI03")
                .status(BookingStatus.PENDING_PAYMENT)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkInBooking(bookingId, hostId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only CONFIRMED");
    }

    @Test
    void completeBooking_checkedIn_transitionsToCompleted() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-CO01")
                .status(BookingStatus.CHECKED_IN)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.completeBooking(bookingId, hostId);

        assertThat(resp.status()).isEqualTo(BookingStatus.COMPLETED);
        verify(kafka).send(eq("booking.completed"), eq(bookingId.toString()));
    }

    @Test
    void completeBooking_notCheckedIn_throws() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-CO02")
                .status(BookingStatus.CONFIRMED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.completeBooking(bookingId, hostId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only CHECKED_IN");
    }

    @Test
    void markNoShow_confirmed_transitionsToNoShow() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-NS01")
                .status(BookingStatus.CONFIRMED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.markNoShow(bookingId, hostId);

        assertThat(resp.status()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    void cancelBooking_alreadyCancelled_throws() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId).guestId(guestId).hostId(hostId).listingId(listingId)
                .checkIn(LocalDateTime.of(2025, 12, 20, 14, 0))
                .checkOut(LocalDateTime.of(2025, 12, 25, 11, 0))
                .guestsCount(2).nights(5).bookingRef("SAF-BKG-TEST3")
                .status(BookingStatus.CANCELLED)
                .baseAmountPaise(2500000L).insuranceAmountPaise(75000L)
                .gstAmountPaise(450000L).totalAmountPaise(3025000L)
                .hostPayoutPaise(2500000L).build();

        when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, guestId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be cancelled");
    }
}
