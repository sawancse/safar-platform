package com.safar.services.repository;

import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.ServiceListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceListingRepository extends JpaRepository<ServiceListing, UUID> {

    Optional<ServiceListing> findByVendorSlug(String vendorSlug);

    boolean existsByVendorSlug(String vendorSlug);

    List<ServiceListing> findByVendorUserId(UUID vendorUserId);

    List<ServiceListing> findByStatus(ServiceListingStatus status);

    List<ServiceListing> findByServiceTypeAndStatus(String serviceType, ServiceListingStatus status);

    List<ServiceListing> findByHasPendingChangesTrue();

    @Query("SELECT l FROM ServiceListing l WHERE l.status = 'VERIFIED' AND l.serviceType = :serviceType AND l.homeCity = :city")
    List<ServiceListing> findVerifiedByTypeAndCity(String serviceType, String city);

    /**
     * Listings VERIFIED on a given date — joins service_listing_availability so
     * we only return listings with status='AVAILABLE' for that date (and not
     * BOOKED/BLACKOUT). Powers The-Knot-style "available on my wedding date"
     * filter — competitive differentiator that no India platform has.
     *
     * NOTE: returns listings that have NO availability row for the date too,
     * since vendors who haven't blocked dates are implicitly available. Only
     * AVAILABLE rows in the calendar table count.
     */
    @Query(value = """
        SELECT l.*
          FROM services.service_listings l
          LEFT JOIN services.service_listing_availability a
                 ON a.service_listing_id = l.id AND a.date = :date
         WHERE l.status = 'VERIFIED'
           AND (:serviceType IS NULL OR l.service_type = :serviceType)
           AND (:city IS NULL OR l.home_city = :city)
           AND (a.id IS NULL OR a.status = 'AVAILABLE')
        """, nativeQuery = true)
    List<ServiceListing> findVerifiedAvailableOn(java.time.LocalDate date, String serviceType, String city);
}
