package com.safar.booking.repository;

import com.safar.booking.entity.UtilityReading;
import com.safar.booking.entity.enums.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, UUID> {

    List<UtilityReading> findByTenancyIdAndUtilityTypeOrderByReadingDateDesc(UUID tenancyId, UtilityType type);

    List<UtilityReading> findByTenancyIdOrderByReadingDateDesc(UUID tenancyId);

    List<UtilityReading> findByTenancyIdAndInvoiceIdIsNull(UUID tenancyId);
}
