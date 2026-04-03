package com.safar.listing.repository;

import com.safar.listing.entity.LocationSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LocationSuggestionRepository extends JpaRepository<LocationSuggestion, UUID> {

    @Query("SELECT ls FROM LocationSuggestion ls WHERE ls.name LIKE CONCAT(:query, '%') " +
           "OR ls.displayName LIKE CONCAT('%', :query, '%') " +
           "ORDER BY ls.popularityScore DESC")
    List<LocationSuggestion> findByNamePrefix(String query);

    List<LocationSuggestion> findByCityOrderByPopularityScoreDesc(String city);

    List<LocationSuggestion> findByTypeOrderByPopularityScoreDesc(String type);
}
