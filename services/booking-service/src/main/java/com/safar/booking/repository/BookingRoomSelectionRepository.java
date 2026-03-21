package com.safar.booking.repository;

import com.safar.booking.entity.BookingRoomSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRoomSelectionRepository extends JpaRepository<BookingRoomSelection, UUID> {
    List<BookingRoomSelection> findByBookingId(UUID bookingId);
}
