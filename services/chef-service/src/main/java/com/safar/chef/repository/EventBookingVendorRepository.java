package com.safar.chef.repository;

import com.safar.chef.entity.EventBookingVendor;
import com.safar.chef.entity.enums.VendorAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventBookingVendorRepository extends JpaRepository<EventBookingVendor, UUID> {

    List<EventBookingVendor> findByEventBookingIdOrderByCreatedAtDesc(UUID eventBookingId);

    Optional<EventBookingVendor> findFirstByEventBookingIdAndStatusNot(UUID eventBookingId,
                                                                      VendorAssignmentStatus excludedStatus);

    List<EventBookingVendor> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    long countByVendorIdAndStatus(UUID vendorId, VendorAssignmentStatus status);
}
