package com.safar.listing.repository;

import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.entity.enums.ListingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByHostId(UUID hostId);
    Page<Listing> findByStatusAndCity(ListingStatus status, String city, Pageable pageable);
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
    List<Listing> findByStatus(ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE l.status = :status" +
           " AND (:city IS NULL OR l.city = :city)" +
           " AND (:type IS NULL OR l.type = :type)" +
           " AND (:minPrice IS NULL OR l.basePricePaise >= :minPrice)" +
           " AND (:maxPrice IS NULL OR l.basePricePaise <= :maxPrice)")
    Page<Listing> searchWithFilters(@Param("status") ListingStatus status,
                                     @Param("city") String city,
                                     @Param("type") ListingType type,
                                     @Param("minPrice") Long minPrice,
                                     @Param("maxPrice") Long maxPrice,
                                     Pageable pageable);

    List<Listing> findByLatAndLng(BigDecimal lat, BigDecimal lng);
}
