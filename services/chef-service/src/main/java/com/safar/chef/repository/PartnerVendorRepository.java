package com.safar.chef.repository;

import com.safar.chef.entity.PartnerVendor;
import com.safar.chef.entity.enums.VendorServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PartnerVendorRepository extends JpaRepository<PartnerVendor, UUID> {

    List<PartnerVendor> findByServiceTypeOrderByRatingAvgDescNullsLastCreatedAtDesc(VendorServiceType serviceType);

    List<PartnerVendor> findByServiceTypeAndActiveTrueOrderByRatingAvgDescNullsLastCreatedAtDesc(VendorServiceType serviceType);

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
