package com.safar.chef.repository;

import com.safar.chef.entity.ChefBooking;
import com.safar.chef.entity.enums.ChefBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ChefBookingRepository extends JpaRepository<ChefBooking, UUID> {

    List<ChefBooking> findByCustomerId(UUID customerId);

    List<ChefBooking> findByChefId(UUID chefId);

    List<ChefBooking> findByChefIdAndServiceDate(UUID chefId, LocalDate serviceDate);

    List<ChefBooking> findByStatus(ChefBookingStatus status);
}
