package com.safar.listing.repository;

import com.safar.listing.entity.VpnListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VpnListingRepository extends JpaRepository<VpnListing, UUID> {

    Optional<VpnListing> findByListingId(UUID listingId);

    @Query("""
            SELECT v FROM VpnListing v
            JOIN Listing l ON l.id = v.listingId
            WHERE v.openToNetwork = true
            AND l.city = :city
            AND (v.availableFrom IS NULL OR v.availableFrom <= CURRENT_DATE)
            AND (v.availableTo IS NULL OR v.availableTo >= CURRENT_DATE)
            """)
    Page<VpnListing> findNetworkListings(@Param("city") String city, Pageable pageable);
}
