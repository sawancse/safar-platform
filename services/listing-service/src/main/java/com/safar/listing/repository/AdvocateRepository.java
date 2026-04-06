package com.safar.listing.repository;

import com.safar.listing.entity.Advocate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdvocateRepository extends JpaRepository<Advocate, UUID> {

    List<Advocate> findByActiveTrue();

    List<Advocate> findByCityAndActiveTrue(String city);
}
