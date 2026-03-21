package com.safar.listing.repository;

import com.safar.listing.entity.SeekerProfile;
import com.safar.listing.entity.enums.GenderPolicy;
import com.safar.listing.entity.enums.ProfileStatus;
import com.safar.listing.entity.enums.SeekerType;
import com.safar.listing.entity.enums.SharingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeekerProfileRepository extends JpaRepository<SeekerProfile, UUID> {

    Page<SeekerProfile> findByStatusAndPreferredCity(ProfileStatus status, String city, Pageable pageable);

    Page<SeekerProfile> findByStatus(ProfileStatus status, Pageable pageable);

    List<SeekerProfile> findByUserId(UUID userId);

    @Query("SELECT s FROM SeekerProfile s WHERE s.status = 'ACTIVE' " +
           "AND s.preferredCity = :city " +
           "AND (:sharing IS NULL OR s.preferredSharing = :sharing) " +
           "AND (:gender IS NULL OR s.genderPreference = :gender) " +
           "AND s.budgetMaxPaise >= :minPrice " +
           "AND (s.budgetMinPaise <= :maxPrice OR :maxPrice = 0)")
    List<SeekerProfile> findMatchingSeekers(String city, SharingType sharing,
                                             GenderPolicy gender, long minPrice, long maxPrice);
}
