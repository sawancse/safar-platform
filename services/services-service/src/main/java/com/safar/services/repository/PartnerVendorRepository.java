package com.safar.services.repository;

import com.safar.services.entity.PartnerVendor;
import com.safar.services.entity.enums.VendorServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PartnerVendorRepository extends JpaRepository<PartnerVendor, UUID> {

    /**
     * NOTE: Spring Data JPA does not parse `NullsLast` as a keyword in derived query
     * method names — it treats it as a property. Use explicit @Query for ordering
     * with NULLS LAST.
     */
    @Query("SELECT v FROM PartnerVendor v WHERE v.serviceType = :serviceType " +
           "ORDER BY v.ratingAvg DESC NULLS LAST, v.createdAt DESC")
    List<PartnerVendor> findByServiceTypeOrderedByRating(@Param("serviceType") VendorServiceType serviceType);

    @Query("SELECT v FROM PartnerVendor v WHERE v.serviceType = :serviceType AND v.active = true " +
           "ORDER BY v.ratingAvg DESC NULLS LAST, v.createdAt DESC")
    List<PartnerVendor> findByServiceTypeActiveOrderedByRating(@Param("serviceType") VendorServiceType serviceType);

    /**
     * Eligible vendors for assignment: active, KYC verified, service_type matches,
     * and either serves anywhere (empty cities array) or includes the booking city
     * (case-insensitive).
     */
    @Query(value = """
        SELECT * FROM chefs.partner_vendors v
        WHERE v.service_type = :serviceType
          AND v.active = TRUE
          AND v.kyc_status = 'VERIFIED'
          AND (
                cardinality(v.service_cities) = 0
             OR LOWER(:city) = ANY(v.service_cities)
          )
        ORDER BY v.rating_avg DESC NULLS LAST, v.jobs_completed DESC, v.created_at DESC
        """, nativeQuery = true)
    List<PartnerVendor> findEligible(@Param("serviceType") String serviceType,
                                     @Param("city") String city);
}
