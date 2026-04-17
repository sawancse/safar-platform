package com.safar.booking.repository;

import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
    List<Booking> findByGuestId(UUID guestId);
    List<Booking> findByHostId(UUID hostId);
    List<Booking> findByListingIdAndStatusIn(UUID listingId, List<BookingStatus> statuses);
    List<Booking> findByGroupBookingId(UUID groupBookingId);
    List<Booking> findByHostIdAndCheckInBetween(UUID hostId, LocalDateTime from, LocalDateTime to);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusInAndCheckOutBefore(List<BookingStatus> statuses, LocalDateTime checkOut);
    Page<Booking> findBySecurityDepositStatusIn(List<String> statuses, Pageable pageable);
}
