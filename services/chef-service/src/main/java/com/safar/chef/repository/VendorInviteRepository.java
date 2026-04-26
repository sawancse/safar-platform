package com.safar.chef.repository;

import com.safar.chef.entity.VendorInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorInviteRepository extends JpaRepository<VendorInvite, UUID> {

    Optional<VendorInvite> findByInviteToken(String inviteToken);

    List<VendorInvite> findByServiceTypeOrderBySentAtDesc(String serviceType);

    List<VendorInvite> findByPhoneOrderBySentAtDesc(String phone);

    List<VendorInvite> findAllByOrderBySentAtDesc();
}
