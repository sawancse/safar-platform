package com.safar.services.repository;

import com.safar.services.entity.EventBooking;
import com.safar.services.entity.enums.EventBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventBookingRepository extends JpaRepository<EventBooking, UUID> {

    List<EventBooking> findByCustomerId(UUID customerId);

    List<EventBooking> findByChefId(UUID chefId);

    List<EventBooking> findByStatus(EventBookingStatus status);

    List<EventBooking> findByChefIdAndEventDate(UUID chefId, LocalDate eventDate);
}
