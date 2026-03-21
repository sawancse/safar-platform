package com.safar.listing.repository;

import com.safar.listing.entity.MedicalStayPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicalStayPackageRepository extends JpaRepository<MedicalStayPackage, UUID> {
    List<MedicalStayPackage> findByHospitalId(UUID hospitalId);
    List<MedicalStayPackage> findByListingId(UUID listingId);
}
