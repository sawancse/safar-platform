package com.safar.supply.repository;

import com.safar.supply.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findByActiveTrueOrderByBusinessNameAsc();

    List<Supplier> findByOrderByCreatedAtDesc();

    @Query(value = """
        SELECT * FROM supply.suppliers s
        WHERE s.active = TRUE
          AND s.kyc_status = 'VERIFIED'
          AND :category = ANY(s.categories)
          AND (
                cardinality(s.service_cities) = 0
             OR LOWER(:city) = ANY(s.service_cities)
          )
        ORDER BY s.rating_avg DESC NULLS LAST, s.pos_completed DESC
        """, nativeQuery = true)
    List<Supplier> findEligible(@Param("category") String category, @Param("city") String city);
}
