package com.safar.listing.controller;

import com.safar.listing.entity.Advocate;
import com.safar.listing.entity.InteriorDesigner;
import com.safar.listing.entity.PartnerBank;
import com.safar.listing.repository.AdvocateRepository;
import com.safar.listing.repository.InteriorDesignerRepository;
import com.safar.listing.repository.PartnerBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD for professional profiles: Advocates (lawyers), Interior Designers, Partner Banks.
 * Public: list (active), get single profile.
 * Admin: create, update, delete.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProfessionalController {

    private final AdvocateRepository advocateRepo;
    private final InteriorDesignerRepository designerRepo;
    private final PartnerBankRepository bankRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // ═══════════════════════════════════════════════════════
    // ── ADVOCATES (Lawyers)
    // ═══════════════════════════════════════════════════════

    @GetMapping("/api/v1/legal/advocates")
    public ResponseEntity<List<Advocate>> listAdvocates(
            @RequestParam(required = false) String city) {
        List<Advocate> list = (city != null && !city.isBlank())
                ? advocateRepo.findByCityAndActiveTrue(city)
                : advocateRepo.findByActiveTrue();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/v1/legal/advocates/{id}")
    public ResponseEntity<Advocate> getAdvocate(@PathVariable UUID id) {
        return advocateRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/legal/advocates")
    public ResponseEntity<Advocate> createAdvocate(
            @RequestBody Advocate advocate,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        advocate.setId(null); // ensure new
        return ResponseEntity.status(HttpStatus.CREATED).body(advocateRepo.save(advocate));
    }

    @PutMapping("/api/v1/legal/advocates/{id}")
    public ResponseEntity<Advocate> updateAdvocate(
            @PathVariable UUID id,
            @RequestBody Advocate update,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Advocate existing = advocateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Advocate not found: " + id));
        if (update.getFullName() != null) existing.setFullName(update.getFullName());
        if (update.getBarCouncilId() != null) existing.setBarCouncilId(update.getBarCouncilId());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getState() != null) existing.setState(update.getState());
        if (update.getExperienceYears() != null) existing.setExperienceYears(update.getExperienceYears());
        if (update.getSpecializations() != null) existing.setSpecializations(update.getSpecializations());
        if (update.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(update.getProfilePhotoUrl());
        if (update.getConsultationFeePaise() != null) existing.setConsultationFeePaise(update.getConsultationFeePaise());
        if (update.getBio() != null) existing.setBio(update.getBio());
        if (update.getVerified() != null) existing.setVerified(update.getVerified());
        if (update.getActive() != null) existing.setActive(update.getActive());
        return ResponseEntity.ok(advocateRepo.save(existing));
    }

    @DeleteMapping("/api/v1/legal/advocates/{id}")
    public ResponseEntity<Void> deleteAdvocate(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Advocate advocate = advocateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Advocate not found: " + id));
        advocate.setActive(false); // soft delete
        advocateRepo.save(advocate);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════
    // ── INTERIOR DESIGNERS
    // ═══════════════════════════════════════════════════════

    @GetMapping("/api/v1/interiors/designers")
    public ResponseEntity<List<InteriorDesigner>> listDesigners(
            @RequestParam(required = false) String city) {
        List<InteriorDesigner> list = (city != null && !city.isBlank())
                ? designerRepo.findByCityAndActiveTrue(city)
                : designerRepo.findByActiveTrue();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/v1/interiors/designers/{id}")
    public ResponseEntity<InteriorDesigner> getDesigner(@PathVariable UUID id) {
        return designerRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/interiors/designers")
    public ResponseEntity<InteriorDesigner> createDesigner(
            @RequestBody InteriorDesigner designer,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        designer.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(designerRepo.save(designer));
    }

    @PutMapping("/api/v1/interiors/designers/{id}")
    public ResponseEntity<InteriorDesigner> updateDesigner(
            @PathVariable UUID id,
            @RequestBody InteriorDesigner update,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        InteriorDesigner existing = designerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Designer not found: " + id));
        if (update.getFullName() != null) existing.setFullName(update.getFullName());
        if (update.getCompanyName() != null) existing.setCompanyName(update.getCompanyName());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getState() != null) existing.setState(update.getState());
        if (update.getExperienceYears() != null) existing.setExperienceYears(update.getExperienceYears());
        if (update.getSpecializations() != null) existing.setSpecializations(update.getSpecializations());
        if (update.getPortfolioUrls() != null) existing.setPortfolioUrls(update.getPortfolioUrls());
        if (update.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(update.getProfilePhotoUrl());
        if (update.getConsultationFeePaise() != null) existing.setConsultationFeePaise(update.getConsultationFeePaise());
        if (update.getBio() != null) existing.setBio(update.getBio());
        if (update.getVerified() != null) existing.setVerified(update.getVerified());
        if (update.getActive() != null) existing.setActive(update.getActive());
        return ResponseEntity.ok(designerRepo.save(existing));
    }

    @DeleteMapping("/api/v1/interiors/designers/{id}")
    public ResponseEntity<Void> deleteDesigner(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        InteriorDesigner designer = designerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Designer not found: " + id));
        designer.setActive(false);
        designerRepo.save(designer);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════
    // ── PARTNER BANKS
    // ═══════════════════════════════════════════════════════

    @GetMapping("/api/v1/homeloan/banks/{id}")
    public ResponseEntity<PartnerBank> getBank(@PathVariable UUID id) {
        return bankRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/homeloan/banks")
    public ResponseEntity<PartnerBank> createBank(
            @RequestBody PartnerBank bank,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        bank.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(bankRepo.save(bank));
    }

    @PutMapping("/api/v1/homeloan/banks/{id}")
    public ResponseEntity<PartnerBank> updateBank(
            @PathVariable UUID id,
            @RequestBody PartnerBank update,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        PartnerBank existing = bankRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found: " + id));
        if (update.getBankName() != null) existing.setBankName(update.getBankName());
        if (update.getLogoUrl() != null) existing.setLogoUrl(update.getLogoUrl());
        if (update.getInterestRateMin() != null) existing.setInterestRateMin(update.getInterestRateMin());
        if (update.getInterestRateMax() != null) existing.setInterestRateMax(update.getInterestRateMax());
        if (update.getMaxTenureMonths() != null) existing.setMaxTenureMonths(update.getMaxTenureMonths());
        if (update.getMaxLoanAmountPaise() != null) existing.setMaxLoanAmountPaise(update.getMaxLoanAmountPaise());
        if (update.getProcessingFeePercent() != null) existing.setProcessingFeePercent(update.getProcessingFeePercent());
        if (update.getSpecialOffers() != null) existing.setSpecialOffers(update.getSpecialOffers());
        if (update.getContactName() != null) existing.setContactName(update.getContactName());
        if (update.getContactEmail() != null) existing.setContactEmail(update.getContactEmail());
        if (update.getContactPhone() != null) existing.setContactPhone(update.getContactPhone());
        if (update.getActive() != null) existing.setActive(update.getActive());
        return ResponseEntity.ok(bankRepo.save(existing));
    }

    @DeleteMapping("/api/v1/homeloan/banks/{id}")
    public ResponseEntity<Void> deleteBank(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        PartnerBank bank = bankRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found: " + id));
        bank.setActive(false);
        bankRepo.save(bank);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════
    // ── SELF-REGISTRATION (Marketplace Model)
    // ═══════════════════════════════════════════════════════

    /**
     * Advocate self-registration. Professional signs up with their own user account.
     * Goes live immediately with "Unverified" badge. Admin reviews later.
     */
    @PostMapping("/api/v1/legal/advocates/register")
    public ResponseEntity<Advocate> registerAdvocate(
            @RequestBody Advocate advocate,
            @RequestHeader("X-User-Id") UUID userId) {
        if (advocateRepo.existsByUserId(userId)) {
            throw new IllegalStateException("You already have an advocate profile");
        }
        advocate.setId(null);
        advocate.setUserId(userId);
        advocate.setVerificationStatus("PENDING");
        advocate.setVerified(false);
        advocate.setActive(true); // visible immediately with unverified badge
        Advocate saved = advocateRepo.save(advocate);
        log.info("New advocate registered: {} (userId={})", saved.getFullName(), userId);
        try { kafkaTemplate.send("professional.registered", saved.getId().toString(), "ADVOCATE:" + userId); } catch (Exception ignored) {}
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Designer self-registration.
     */
    @PostMapping("/api/v1/interiors/designers/register")
    public ResponseEntity<InteriorDesigner> registerDesigner(
            @RequestBody InteriorDesigner designer,
            @RequestHeader("X-User-Id") UUID userId) {
        if (designerRepo.existsByUserId(userId)) {
            throw new IllegalStateException("You already have a designer profile");
        }
        designer.setId(null);
        designer.setUserId(userId);
        designer.setVerificationStatus("PENDING");
        designer.setVerified(false);
        designer.setActive(true);
        InteriorDesigner saved = designerRepo.save(designer);
        log.info("New designer registered: {} (userId={})", saved.getFullName(), userId);
        try { kafkaTemplate.send("professional.registered", saved.getId().toString(), "DESIGNER:" + userId); } catch (Exception ignored) {}
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ═══════════════════════════════════════════════════════
    // ── ADMIN VERIFICATION
    // ═══════════════════════════════════════════════════════

    /** List pending advocate registrations */
    @GetMapping("/api/v1/legal/advocates/pending")
    public ResponseEntity<List<Advocate>> getPendingAdvocates(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(advocateRepo.findByVerificationStatus("PENDING"));
    }

    /** Approve advocate */
    @PostMapping("/api/v1/legal/advocates/{id}/approve")
    public ResponseEntity<Advocate> approveAdvocate(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Advocate adv = advocateRepo.findById(id).orElseThrow(() -> new RuntimeException("Advocate not found"));
        adv.setVerificationStatus("APPROVED");
        adv.setVerified(true);
        adv.setVerifiedBy(adminId);
        adv.setVerifiedAt(OffsetDateTime.now());
        adv.setRejectionReason(null);
        Advocate saved = advocateRepo.save(adv);
        log.info("Advocate {} approved by admin {}", saved.getFullName(), adminId);
        try { kafkaTemplate.send("professional.approved", saved.getId().toString(), "ADVOCATE:" + saved.getUserId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    /** Reject advocate */
    @PostMapping("/api/v1/legal/advocates/{id}/reject")
    public ResponseEntity<Advocate> rejectAdvocate(
            @PathVariable UUID id,
            @RequestParam String reason,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Advocate adv = advocateRepo.findById(id).orElseThrow(() -> new RuntimeException("Advocate not found"));
        adv.setVerificationStatus("REJECTED");
        adv.setVerified(false);
        adv.setActive(false);
        adv.setRejectionReason(reason);
        adv.setVerifiedBy(adminId);
        adv.setVerifiedAt(OffsetDateTime.now());
        Advocate saved = advocateRepo.save(adv);
        log.info("Advocate {} rejected by admin {}: {}", saved.getFullName(), adminId, reason);
        try { kafkaTemplate.send("professional.rejected", saved.getId().toString(), "ADVOCATE:" + saved.getUserId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    /** List pending designer registrations */
    @GetMapping("/api/v1/interiors/designers/pending")
    public ResponseEntity<List<InteriorDesigner>> getPendingDesigners(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(designerRepo.findByVerificationStatus("PENDING"));
    }

    /** Approve designer */
    @PostMapping("/api/v1/interiors/designers/{id}/approve")
    public ResponseEntity<InteriorDesigner> approveDesigner(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        InteriorDesigner d = designerRepo.findById(id).orElseThrow(() -> new RuntimeException("Designer not found"));
        d.setVerificationStatus("APPROVED");
        d.setVerified(true);
        d.setVerifiedBy(adminId);
        d.setVerifiedAt(OffsetDateTime.now());
        d.setRejectionReason(null);
        InteriorDesigner saved = designerRepo.save(d);
        log.info("Designer {} approved by admin {}", saved.getFullName(), adminId);
        try { kafkaTemplate.send("professional.approved", saved.getId().toString(), "DESIGNER:" + saved.getUserId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    /** Reject designer */
    @PostMapping("/api/v1/interiors/designers/{id}/reject")
    public ResponseEntity<InteriorDesigner> rejectDesigner(
            @PathVariable UUID id,
            @RequestParam String reason,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        InteriorDesigner d = designerRepo.findById(id).orElseThrow(() -> new RuntimeException("Designer not found"));
        d.setVerificationStatus("REJECTED");
        d.setVerified(false);
        d.setActive(false);
        d.setRejectionReason(reason);
        d.setVerifiedBy(adminId);
        d.setVerifiedAt(OffsetDateTime.now());
        InteriorDesigner saved = designerRepo.save(d);
        log.info("Designer {} rejected by admin {}: {}", saved.getFullName(), adminId, reason);
        try { kafkaTemplate.send("professional.rejected", saved.getId().toString(), "DESIGNER:" + saved.getUserId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    // ═══════════════════════════════════════════════════════
    // ── PROFESSIONAL DASHBOARD (Self-manage)
    // ═══════════════════════════════════════════════════════

    /** Get my advocate profile */
    @GetMapping("/api/v1/legal/advocates/my-profile")
    public ResponseEntity<Advocate> getMyAdvocateProfile(@RequestHeader("X-User-Id") UUID userId) {
        return advocateRepo.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Update my advocate profile */
    @PutMapping("/api/v1/legal/advocates/my-profile")
    public ResponseEntity<Advocate> updateMyAdvocateProfile(
            @RequestBody Advocate update,
            @RequestHeader("X-User-Id") UUID userId) {
        Advocate existing = advocateRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No advocate profile found for this user"));
        // Only allow editing own profile fields (not verification status)
        if (update.getFullName() != null) existing.setFullName(update.getFullName());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getState() != null) existing.setState(update.getState());
        if (update.getExperienceYears() != null) existing.setExperienceYears(update.getExperienceYears());
        if (update.getSpecializations() != null) existing.setSpecializations(update.getSpecializations());
        if (update.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(update.getProfilePhotoUrl());
        if (update.getConsultationFeePaise() != null) existing.setConsultationFeePaise(update.getConsultationFeePaise());
        if (update.getBio() != null) existing.setBio(update.getBio());
        if (update.getLanguages() != null) existing.setLanguages(update.getLanguages());
        if (update.getAvailableDays() != null) existing.setAvailableDays(update.getAvailableDays());
        if (update.getAvailableHours() != null) existing.setAvailableHours(update.getAvailableHours());
        if (update.getIdProofUrl() != null) existing.setIdProofUrl(update.getIdProofUrl());
        if (update.getLicenseUrl() != null) existing.setLicenseUrl(update.getLicenseUrl());
        if (update.getCertificateUrls() != null) existing.setCertificateUrls(update.getCertificateUrls());
        return ResponseEntity.ok(advocateRepo.save(existing));
    }

    /** Get my designer profile */
    @GetMapping("/api/v1/interiors/designers/my-profile")
    public ResponseEntity<InteriorDesigner> getMyDesignerProfile(@RequestHeader("X-User-Id") UUID userId) {
        return designerRepo.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Update my designer profile */
    @PutMapping("/api/v1/interiors/designers/my-profile")
    public ResponseEntity<InteriorDesigner> updateMyDesignerProfile(
            @RequestBody InteriorDesigner update,
            @RequestHeader("X-User-Id") UUID userId) {
        InteriorDesigner existing = designerRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No designer profile found for this user"));
        if (update.getFullName() != null) existing.setFullName(update.getFullName());
        if (update.getCompanyName() != null) existing.setCompanyName(update.getCompanyName());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getCity() != null) existing.setCity(update.getCity());
        if (update.getState() != null) existing.setState(update.getState());
        if (update.getExperienceYears() != null) existing.setExperienceYears(update.getExperienceYears());
        if (update.getSpecializations() != null) existing.setSpecializations(update.getSpecializations());
        if (update.getPortfolioUrls() != null) existing.setPortfolioUrls(update.getPortfolioUrls());
        if (update.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(update.getProfilePhotoUrl());
        if (update.getConsultationFeePaise() != null) existing.setConsultationFeePaise(update.getConsultationFeePaise());
        if (update.getBio() != null) existing.setBio(update.getBio());
        if (update.getIiidMembership() != null) existing.setIiidMembership(update.getIiidMembership());
        if (update.getGstNumber() != null) existing.setGstNumber(update.getGstNumber());
        if (update.getServiceAreas() != null) existing.setServiceAreas(update.getServiceAreas());
        if (update.getMinBudgetPaise() != null) existing.setMinBudgetPaise(update.getMinBudgetPaise());
        if (update.getIdProofUrl() != null) existing.setIdProofUrl(update.getIdProofUrl());
        if (update.getLicenseUrl() != null) existing.setLicenseUrl(update.getLicenseUrl());
        if (update.getCertificateUrls() != null) existing.setCertificateUrls(update.getCertificateUrls());
        return ResponseEntity.ok(designerRepo.save(existing));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
