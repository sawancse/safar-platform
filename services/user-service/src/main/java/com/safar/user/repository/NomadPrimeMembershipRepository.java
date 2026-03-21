package com.safar.user.repository;

import com.safar.user.entity.NomadPrimeMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NomadPrimeMembershipRepository extends JpaRepository<NomadPrimeMembership, UUID> {
    Optional<NomadPrimeMembership> findByGuestIdAndStatus(UUID guestId, String status);
    boolean existsByGuestIdAndStatus(UUID guestId, String status);
    List<NomadPrimeMembership> findByNextRenewalDate(LocalDate date);
}
