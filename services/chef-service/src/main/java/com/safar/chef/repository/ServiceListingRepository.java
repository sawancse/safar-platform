package com.safar.chef.repository;

import com.safar.chef.entity.ServiceListing;
import com.safar.chef.entity.enums.ServiceListingStatus;
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

    @Query("SELECT l FROM ServiceListing l WHERE l.status = 'VERIFIED' AND l.serviceType = :serviceType AND l.homeCity = :city")
    List<ServiceListing> findVerifiedByTypeAndCity(String serviceType, String city);
}
