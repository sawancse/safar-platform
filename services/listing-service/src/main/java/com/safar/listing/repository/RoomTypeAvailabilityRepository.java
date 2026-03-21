package com.safar.listing.repository;

import com.safar.listing.entity.RoomTypeAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeAvailabilityRepository extends JpaRepository<RoomTypeAvailability, UUID> {

    List<RoomTypeAvailability> findByRoomTypeIdAndDateBetween(UUID roomTypeId, LocalDate from, LocalDate to);

    Optional<RoomTypeAvailability> findByRoomTypeIdAndDate(UUID roomTypeId, LocalDate date);
}
