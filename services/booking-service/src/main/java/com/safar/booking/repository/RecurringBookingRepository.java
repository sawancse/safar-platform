package com.safar.booking.repository;

import com.safar.booking.entity.RecurringBooking;
import com.safar.booking.entity.enums.RecurringStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, UUID> {
    List<RecurringBooking> findByNextBookingDateAndStatus(LocalDate nextBookingDate, RecurringStatus status);
    List<RecurringBooking> findByGuestId(UUID guestId);
    Optional<RecurringBooking> findByIdAndGuestId(UUID id, UUID guestId);
}
