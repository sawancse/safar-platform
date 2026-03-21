package com.safar.listing.repository;

import com.safar.listing.entity.HospitalProcedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HospitalProcedureRepository extends JpaRepository<HospitalProcedure, UUID> {
    List<HospitalProcedure> findByHospitalId(UUID hospitalId);
    List<HospitalProcedure> findBySpecialty(String specialty);
    List<HospitalProcedure> findByProcedureNameContainingIgnoreCase(String name);
    List<HospitalProcedure> findByHospitalIdAndSpecialty(UUID hospitalId, String specialty);
}
