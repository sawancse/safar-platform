package com.safar.chef.repository;

import com.safar.chef.entity.EventBookingStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventBookingStaffRepository extends JpaRepository<EventBookingStaff, UUID> {

    List<EventBookingStaff> findByBookingId(UUID bookingId);

    void deleteByBookingId(UUID bookingId);

    List<EventBookingStaff> findByStaffId(UUID staffId);
}
