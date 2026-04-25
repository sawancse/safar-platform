package com.safar.chef.service;

import com.safar.chef.dto.PartnerVendorRequest;
import com.safar.chef.entity.PartnerVendor;
import com.safar.chef.entity.enums.VendorServiceType;
import com.safar.chef.repository.PartnerVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerVendorService {

    private final PartnerVendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public List<PartnerVendor> listByType(VendorServiceType type, boolean activeOnly) {
        return activeOnly
                ? vendorRepo.findByServiceTypeAndActiveTrueOrderByRatingAvgDescNullsLastCreatedAtDesc(type)
                : vendorRepo.findByServiceTypeOrderByRatingAvgDescNullsLastCreatedAtDesc(type);
    }

    @Transactional(readOnly = true)
    public List<PartnerVendor> findEligible(VendorServiceType type, String city) {
        if (city == null || city.isBlank()) {
            return vendorRepo.findByServiceTypeAndActiveTrueOrderByRatingAvgDescNullsLastCreatedAtDesc(type);
        }
        return vendorRepo.findEligible(type.name(), city.trim());
    }

    @Transactional(readOnly = true)
    public PartnerVendor get(UUID id) {
        return vendorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));
    }

    @Transactional
    public PartnerVendor create(PartnerVendorRequest req) {
        if (req.serviceType() == null) throw new IllegalArgumentException("serviceType required");
        if (req.businessName() == null || req.businessName().isBlank())
            throw new IllegalArgumentException("businessName required");
        if (req.phone() == null || req.phone().isBlank())
            throw new IllegalArgumentException("phone required");

        PartnerVendor v = PartnerVendor.builder()
                .serviceType(req.serviceType())
                .businessName(req.businessName())
                .ownerName(req.ownerName())
                .phone(req.phone())
                .email(req.email())
                .whatsapp(req.whatsapp())
                .gst(req.gst())
                .pan(req.pan())
                .bankAccount(req.bankAccount())
                .bankIfsc(req.bankIfsc())
                .bankHolder(req.bankHolder())
                .address(req.address())
                .serviceCities(toLowerArray(req.serviceCities()))
                .serviceRadiusKm(req.serviceRadiusKm() == null ? 25 : req.serviceRadiusKm())
                .portfolioJson(req.portfolioJson())
                .pricingOverrideJson(req.pricingOverrideJson())
                .kycStatus(req.kycStatus() == null ? "PENDING" : req.kycStatus())
                .kycNotes(req.kycNotes())
                .notes(req.notes())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .build();
        return vendorRepo.save(v);
    }

    @Transactional
    public PartnerVendor update(UUID id, PartnerVendorRequest req) {
        PartnerVendor v = get(id);
        if (req.serviceType() != null)         v.setServiceType(req.serviceType());
        if (req.businessName() != null)        v.setBusinessName(req.businessName());
        if (req.ownerName() != null)           v.setOwnerName(req.ownerName());
        if (req.phone() != null)               v.setPhone(req.phone());
        if (req.email() != null)               v.setEmail(req.email());
        if (req.whatsapp() != null)            v.setWhatsapp(req.whatsapp());
        if (req.gst() != null)                 v.setGst(req.gst());
        if (req.pan() != null)                 v.setPan(req.pan());
        if (req.bankAccount() != null)         v.setBankAccount(req.bankAccount());
        if (req.bankIfsc() != null)            v.setBankIfsc(req.bankIfsc());
        if (req.bankHolder() != null)          v.setBankHolder(req.bankHolder());
        if (req.address() != null)             v.setAddress(req.address());
        if (req.serviceCities() != null)       v.setServiceCities(toLowerArray(req.serviceCities()));
        if (req.serviceRadiusKm() != null)     v.setServiceRadiusKm(req.serviceRadiusKm());
        if (req.portfolioJson() != null)       v.setPortfolioJson(req.portfolioJson());
        if (req.pricingOverrideJson() != null) v.setPricingOverrideJson(req.pricingOverrideJson());
        if (req.kycStatus() != null)           v.setKycStatus(req.kycStatus());
        if (req.kycNotes() != null)            v.setKycNotes(req.kycNotes());
        if (req.notes() != null)               v.setNotes(req.notes());
        if (req.active() != null)              v.setActive(req.active());
        return vendorRepo.save(v);
    }

    @Transactional
    public PartnerVendor setActive(UUID id, boolean active) {
        PartnerVendor v = get(id);
        v.setActive(active);
        return vendorRepo.save(v);
    }

    @Transactional
    public PartnerVendor verifyKyc(UUID id, boolean verified, String notes) {
        PartnerVendor v = get(id);
        v.setKycStatus(verified ? "VERIFIED" : "REJECTED");
        if (notes != null) v.setKycNotes(notes);
        return vendorRepo.save(v);
    }

    /**
     * Recompute rolling rating: new_avg = (old_avg * old_count + new_stars) / (old_count + 1).
     * Called when a customer rates a delivered vendor assignment.
     */
    @Transactional
    public void bumpRating(UUID vendorId, int stars) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1-5");
        PartnerVendor v = get(vendorId);
        int oldCount = v.getRatingCount() == null ? 0 : v.getRatingCount();
        BigDecimal oldAvg = v.getRatingAvg() == null ? BigDecimal.ZERO : v.getRatingAvg();
        BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(oldCount))
                .add(BigDecimal.valueOf(stars))
                .divide(BigDecimal.valueOf(oldCount + 1L), 2, RoundingMode.HALF_UP);
        v.setRatingAvg(newAvg);
        v.setRatingCount(oldCount + 1);
        vendorRepo.save(v);
    }

    @Transactional
    public void incrementJobsCompleted(UUID vendorId) {
        PartnerVendor v = get(vendorId);
        v.setJobsCompleted((v.getJobsCompleted() == null ? 0 : v.getJobsCompleted()) + 1);
        vendorRepo.save(v);
    }

    private String[] toLowerArray(List<String> cities) {
        if (cities == null) return new String[0];
        return cities.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toArray(String[]::new);
    }
}
