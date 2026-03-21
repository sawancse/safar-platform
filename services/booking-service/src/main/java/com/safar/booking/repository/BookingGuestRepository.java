package com.safar.booking.repository;

import com.safar.booking.entity.BookingGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingGuestRepository extends JpaRepository<BookingGuest, UUID> {
    List<BookingGuest> findByBookingId(UUID bookingId);
}
