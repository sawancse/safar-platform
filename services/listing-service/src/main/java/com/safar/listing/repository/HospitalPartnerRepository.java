package com.safar.listing.repository;

import com.safar.listing.entity.HospitalPartner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HospitalPartnerRepository extends JpaRepository<HospitalPartner, UUID> {
    List<HospitalPartner> findByCity(String city);
    Page<HospitalPartner> findByCity(String city, Pageable pageable);
    List<HospitalPartner> findBySpecialtiesContaining(String specialty);
    List<HospitalPartner> findByCityAndSpecialtiesContaining(String city, String specialty);
}
