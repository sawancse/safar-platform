package com.safar.listing.repository;

import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.enums.SalePropertyStatus;
import com.safar.listing.entity.enums.SalePropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SalePropertyRepository extends JpaRepository<SaleProperty, UUID> {

    Page<SaleProperty> findBySellerId(UUID sellerId, Pageable pageable);

    Page<SaleProperty> findBySellerIdAndStatus(UUID sellerId, SalePropertyStatus status, Pageable pageable);

    List<SaleProperty> findByStatus(SalePropertyStatus status);

    Page<SaleProperty> findByStatus(SalePropertyStatus status, Pageable pageable);

    @Query("SELECT sp FROM SaleProperty sp WHERE sp.status = :status " +
            "AND (:city IS NULL OR sp.city = :city) " +
            "AND (:type IS NULL OR sp.salePropertyType = :type) " +
            "AND (:minPrice IS NULL OR sp.askingPricePaise >= :minPrice) " +
            "AND (:maxPrice IS NULL OR sp.askingPricePaise <= :maxPrice) " +
            "AND (:bedrooms IS NULL OR sp.bedrooms = :bedrooms)")
    Page<SaleProperty> searchWithFilters(
            @Param("status") SalePropertyStatus status,
            @Param("city") String city,
            @Param("type") SalePropertyType type,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("bedrooms") Integer bedrooms,
            Pageable pageable);

    long countBySellerIdAndStatus(UUID sellerId, SalePropertyStatus status);

    @Query("SELECT sp FROM SaleProperty sp WHERE sp.status = 'ACTIVE' " +
            "AND sp.city IN :cities AND sp.askingPricePaise BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY sp.createdAt DESC")
    List<SaleProperty> findMatchingProperties(
            @Param("cities") List<String> cities,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable);

    @Query("SELECT sp FROM SaleProperty sp WHERE sp.status = 'ACTIVE' " +
            "AND sp.city = :city AND sp.salePropertyType = :type " +
            "AND sp.bedrooms = :bedrooms AND sp.id != :excludeId " +
            "ORDER BY ABS(sp.askingPricePaise - :price)")
    List<SaleProperty> findSimilarProperties(
            @Param("city") String city,
            @Param("type") SalePropertyType type,
            @Param("bedrooms") Integer bedrooms,
            @Param("price") Long price,
            @Param("excludeId") UUID excludeId,
            Pageable pageable);

    @Query("SELECT sp FROM SaleProperty sp WHERE sp.status = 'ACTIVE' AND sp.expiresAt < CURRENT_TIMESTAMP")
    List<SaleProperty> findExpiredProperties();
}
