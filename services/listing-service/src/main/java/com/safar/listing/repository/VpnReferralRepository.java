package com.safar.listing.repository;

import com.safar.listing.entity.VpnReferral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VpnReferralRepository extends JpaRepository<VpnReferral, UUID> {
    Page<VpnReferral> findByReferrerId(UUID referrerId, Pageable pageable);
}
