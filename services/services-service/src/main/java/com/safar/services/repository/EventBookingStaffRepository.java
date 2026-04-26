package com.safar.services.repository;

import com.safar.services.entity.EventBookingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventBookingStaffRepository extends JpaRepository<EventBookingStaff, UUID> {

    List<EventBookingStaff> findByBookingId(UUID bookingId);

    void deleteByBookingId(UUID bookingId);

    List<EventBookingStaff> findByStaffId(UUID staffId);

    /**
     * Returns [role, avgRating, count] for every role that has at least one
     * rated assignment. Feeds the /aggregate-ratings public endpoint shown
     * on the /cooks/services landing.
     */
    @Query("SELECT ebs.role, AVG(ebs.rating), COUNT(ebs.rating) FROM EventBookingStaff ebs " +
           "WHERE ebs.rating IS NOT NULL AND ebs.rating > 0 GROUP BY ebs.role")
    List<Object[]> aggregateRatingsByRole();
}
