package com.safar.listing.repository;

import com.safar.listing.entity.LocalityPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalityPolygonRepository extends JpaRepository<LocalityPolygon, UUID> {

    List<LocalityPolygon> findByCityAndActiveTrue(String city);

    Optional<LocalityPolygon> findByNameAndCity(String name, String city);

    List<LocalityPolygon> findByActiveTrue();

    boolean existsByNameAndCity(String name, String city);
}
