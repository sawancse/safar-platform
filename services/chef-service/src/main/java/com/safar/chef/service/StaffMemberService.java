package com.safar.chef.service;

import com.safar.chef.dto.StaffMemberRequest;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.StaffMember;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffMemberService {

    private final StaffMemberRepository staffRepo;
    private final ChefProfileRepository chefProfileRepo;

    private UUID resolveChefId(UUID userId) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No chef profile for user " + userId));
        return chef.getId();
    }

    @Transactional(readOnly = true)
    public List<StaffMember> listMyStaff(UUID userId, boolean activeOnly) {
        UUID chefId = resolveChefId(userId);
        return activeOnly
                ? staffRepo.findByChefIdAndActiveTrueOrderByCreatedAtDesc(chefId)
                : staffRepo.findByChefIdOrderByCreatedAtDesc(chefId);
    }

    /**
     * Chef view of "people I can assign": own team + platform pool.
     * Both groups share the same StaffMember shape; chef_id=null identifies
     * a pool member. Active only.
     */
    @Transactional(readOnly = true)
    public List<StaffMember> listAssignable(UUID userId) {
        UUID chefId = resolveChefId(userId);
        List<StaffMember> mine = staffRepo.findByChefIdAndActiveTrueOrderByCreatedAtDesc(chefId);
        List<StaffMember> pool = staffRepo.findByChefIdIsNullAndActiveTrueOrderByCreatedAtDesc();
        List<StaffMember> combined = new java.util.ArrayList<>(mine.size() + pool.size());
        combined.addAll(mine);
        combined.addAll(pool);
        return combined;
    }

    @Transactional(readOnly = true)
    public List<StaffMember> listStaffByRole(UUID userId, String role) {
        UUID chefId = resolveChefId(userId);
        List<StaffMember> mine = staffRepo.findByChefIdAndRoleAndActiveTrue(chefId, role);
        List<StaffMember> pool = staffRepo.findByChefIdIsNullAndRoleAndActiveTrue(role);
        List<StaffMember> combined = new java.util.ArrayList<>(mine.size() + pool.size());
        combined.addAll(mine);
        combined.addAll(pool);
        return combined;
    }

    @Transactional
    public StaffMember create(UUID userId, StaffMemberRequest req) {
        UUID chefId = resolveChefId(userId);
        StaffMember member = StaffMember.builder()
                .chefId(chefId)
                .name(req.name())
                .role(req.role())
                .phone(req.phone())
                .photoUrl(req.photoUrl())
                .hourlyRatePaise(req.hourlyRatePaise())
                .languages(req.languages())
                .yearsExperience(req.yearsExperience())
                .notes(req.notes())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .build();
        return staffRepo.save(member);
    }

    @Transactional
    public StaffMember update(UUID userId, UUID staffId, StaffMemberRequest req) {
        UUID chefId = resolveChefId(userId);
        StaffMember member = staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (!chefId.equals(member.getChefId())) {
            throw new AccessDeniedException("Not your staff member");
        }
        if (req.name() != null)             member.setName(req.name());
        if (req.role() != null)             member.setRole(req.role());
        if (req.phone() != null)            member.setPhone(req.phone());
        if (req.photoUrl() != null)         member.setPhotoUrl(req.photoUrl());
        if (req.hourlyRatePaise() != null)  member.setHourlyRatePaise(req.hourlyRatePaise());
        if (req.languages() != null)        member.setLanguages(req.languages());
        if (req.yearsExperience() != null)  member.setYearsExperience(req.yearsExperience());
        if (req.notes() != null)            member.setNotes(req.notes());
        if (req.active() != null)           member.setActive(req.active());
        return staffRepo.save(member);
    }

    @Transactional
    public void softDelete(UUID userId, UUID staffId) {
        UUID chefId = resolveChefId(userId);
        StaffMember member = staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (!chefId.equals(member.getChefId())) {
            throw new AccessDeniedException("Not your staff member");
        }
        member.setActive(false);
        staffRepo.save(member);
    }
}
