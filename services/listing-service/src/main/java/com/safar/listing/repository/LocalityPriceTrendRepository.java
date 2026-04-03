package com.safar.listing.repository;

import com.safar.listing.entity.LocalityPriceTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LocalityPriceTrendRepository extends JpaRepository<LocalityPriceTrend, UUID> {

    @Query("SELECT lpt FROM LocalityPriceTrend lpt WHERE lpt.city = :city " +
            "AND lpt.locality = :locality ORDER BY lpt.month DESC")
    List<LocalityPriceTrend> findByCityAndLocality(@Param("city") String city, @Param("locality") String locality);

    @Query("SELECT lpt FROM LocalityPriceTrend lpt WHERE lpt.city = :city " +
            "AND lpt.month = (SELECT MAX(l2.month) FROM LocalityPriceTrend l2 WHERE l2.city = :city) " +
            "ORDER BY lpt.avgPricePerSqftPaise DESC")
    List<LocalityPriceTrend> findLatestByCityOrderByPrice(@Param("city") String city);
}
