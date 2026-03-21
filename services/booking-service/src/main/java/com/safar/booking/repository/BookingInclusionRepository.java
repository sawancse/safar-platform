package com.safar.booking.repository;

import com.safar.booking.entity.BookingInclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingInclusionRepository extends JpaRepository<BookingInclusion, UUID> {

    List<BookingInclusion> findByBookingId(UUID bookingId);

    void deleteByBookingId(UUID bookingId);
}
