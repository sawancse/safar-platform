package com.safar.chef.repository;

import com.safar.chef.entity.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    List<StaffMember> findByChefIdOrderByCreatedAtDesc(UUID chefId);

    List<StaffMember> findByChefIdAndActiveTrueOrderByCreatedAtDesc(UUID chefId);

    List<StaffMember> findByChefIdAndRoleAndActiveTrue(UUID chefId, String role);

    // ── Platform pool (chef_id IS NULL) ────────────────────────────────
    List<StaffMember> findByChefIdIsNullOrderByCreatedAtDesc();

    List<StaffMember> findByChefIdIsNullAndActiveTrueOrderByCreatedAtDesc();

    List<StaffMember> findByChefIdIsNullAndRoleAndActiveTrue(String role);
}
