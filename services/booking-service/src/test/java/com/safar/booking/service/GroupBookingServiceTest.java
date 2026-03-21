package com.safar.booking.service;

import com.safar.booking.dto.GroupBookingRequest;
import com.safar.booking.dto.GroupBookingResult;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupBookingServiceTest {

    @Mock BookingRepository bookingRepo;
    @Mock StringRedisTemplate redis;
    @Mock KafkaTemplate<String, String> kafka;
    @Mock ListingServiceClient listingClient;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks GroupBookingService groupBookingService;

    private final UUID guestId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId1 = UUID.randomUUID();
    private final UUID listingId2 = UUID.randomUUID();
    private final LocalDateTime checkIn = LocalDateTime.of(2025, 12, 20, 14, 0);
    private final LocalDateTime checkOut = LocalDateTime.of(2025, 12, 25, 14, 0);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void createGroupBooking_createsMultipleBookings() {
        GroupBookingRequest req = new GroupBookingRequest(
                groupId, List.of(listingId1, listingId2), checkIn, checkOut, 2);

        when(listingClient.getGroupListingIds(groupId)).thenReturn(List.of(listingId1, listingId2, UUID.randomUUID()));
        when(listingClient.getGroupBundleDiscountPct(groupId)).thenReturn(10);
        when(listingClient.isAvailable(eq(listingId1), any(), any())).thenReturn(true);
        when(listingClient.isAvailable(eq(listingId2), any(), any())).thenReturn(true);
        when(listingClient.getBasePricePaise(listingId1)).thenReturn(500000L);
        when(listingClient.getBasePricePaise(listingId2)).thenReturn(300000L);
        when(listingClient.getHostId(listingId1)).thenReturn(hostId);
        when(listingClient.getHostId(listingId2)).thenReturn(hostId);
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        GroupBookingResult result = groupBookingService.createGroupBooking(guestId, req);

        assertThat(result.bookingIds()).hasSize(2);
        assertThat(result.groupBookingId()).isNotNull();
        assertThat(result.totalAmountPaise()).isGreaterThan(0);
        // No discount because not all group listings were booked (group has 3, only 2 booked)
        assertThat(result.discountApplied()).isFalse();
        verify(bookingRepo, times(2)).save(any(Booking.class));
        verify(kafka, times(2)).send(eq("booking.created"), anyString());
    }

    @Test
    void createGroupBooking_bundleDiscountApplied_whenAllGroupListingsBooked() {
        // Group has exactly 2 listings; booking requests all 2 => discount applies
        GroupBookingRequest req = new GroupBookingRequest(
                groupId, List.of(listingId1, listingId2), checkIn, checkOut, 2);

        when(listingClient.getGroupListingIds(groupId)).thenReturn(List.of(listingId1, listingId2));
        when(listingClient.getGroupBundleDiscountPct(groupId)).thenReturn(10);
        when(listingClient.isAvailable(eq(listingId1), any(), any())).thenReturn(true);
        when(listingClient.isAvailable(eq(listingId2), any(), any())).thenReturn(true);
        when(listingClient.getBasePricePaise(listingId1)).thenReturn(500000L); // Rs 5,000/night
        when(listingClient.getBasePricePaise(listingId2)).thenReturn(500000L); // Rs 5,000/night
        when(listingClient.getHostId(listingId1)).thenReturn(hostId);
        when(listingClient.getHostId(listingId2)).thenReturn(hostId);
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        GroupBookingResult result = groupBookingService.createGroupBooking(guestId, req);

        assertThat(result.discountApplied()).isTrue();
        assertThat(result.bookingIds()).hasSize(2);

        // Verify discount was applied: each listing has base 500000*5=2500000, 10% off = 2250000
        // insurance = min(15000*5, 150000) = 75000
        // gst = round(2250000 * 0.18) = 405000
        // per booking total = 2250000 + 75000 + 405000 = 2730000
        // total for 2 bookings = 5460000
        assertThat(result.totalAmountPaise()).isEqualTo(5460000L);
        verify(bookingRepo, times(2)).save(any(Booking.class));
    }
}
