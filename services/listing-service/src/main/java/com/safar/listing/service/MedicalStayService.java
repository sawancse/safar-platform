package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.HospitalPartner;
import com.safar.listing.entity.HospitalProcedure;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.MedicalStayPackage;
import com.safar.listing.repository.HospitalPartnerRepository;
import com.safar.listing.repository.HospitalProcedureRepository;
import com.safar.listing.repository.ListingMediaRepository;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.MedicalStayPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalStayService {

    private final HospitalPartnerRepository hospitalRepository;
    private final MedicalStayPackageRepository packageRepository;
    private final HospitalProcedureRepository procedureRepository;
    private final ListingRepository listingRepository;
    private final ListingMediaRepository listingMediaRepository;

    @Transactional
    public HospitalPartner registerHospital(RegisterHospitalRequest req) {
        HospitalPartner hospital = HospitalPartner.builder()
                .name(req.name())
                .city(req.city())
                .address(req.address())
                .lat(req.lat())
                .lng(req.lng())
                .specialties(req.specialties() != null ? req.specialties() : "")
                .accreditations(req.accreditations() != null ? req.accreditations() : "")
                .contactEmail(req.contactEmail())
                .active(true)
                .build();

        HospitalPartner saved = hospitalRepository.save(hospital);
        log.info("Hospital partner {} registered: {}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public MedicalStayPackage registerPackage(UUID hostId, UUID listingId, MedicalPackageRequest req) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalStateException("Only the listing host can register a medical package");
        }

        HospitalPartner hospital = hospitalRepository.findById(req.hospitalId())
                .orElseThrow(() -> new NoSuchElementException("Hospital not found: " + req.hospitalId()));

        BigDecimal distance = null;
        if (listing.getLat() != null && listing.getLng() != null
                && hospital.getLat() != null && hospital.getLng() != null) {
            distance = BigDecimal.valueOf(haversineKm(
                    listing.getLat().doubleValue(), listing.getLng().doubleValue(),
                    hospital.getLat().doubleValue(), hospital.getLng().doubleValue()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        MedicalStayPackage pkg = MedicalStayPackage.builder()
                .listingId(listingId)
                .hospitalId(req.hospitalId())
                .distanceKm(distance)
                .includesPickup(req.includesPickup())
                .includesTranslator(req.includesTranslator())
                .caregiverFriendly(req.caregiverFriendly())
                .medicalPricePaise(req.medicalPricePaise())
                .minStayNights(req.minStayNights())
                .build();

        MedicalStayPackage saved = packageRepository.save(pkg);

        listing.setMedicalStay(true);
        listingRepository.save(listing);

        log.info("Medical stay package {} registered for listing {} with hospital {}",
                saved.getId(), listingId, req.hospitalId());
        return saved;
    }

    public List<MedicalStayPackageDto> searchPackages(String city, String specialty) {
        List<MedicalStayPackage> packages;
        if (city != null && !city.isBlank() && specialty != null && !specialty.isBlank()) {
            List<HospitalPartner> hospitals = hospitalRepository.findByCityAndSpecialtiesContaining(city, specialty);
            packages = hospitals.stream()
                    .flatMap(h -> packageRepository.findByHospitalId(h.getId()).stream())
                    .toList();
        } else if (city != null && !city.isBlank()) {
            List<HospitalPartner> hospitals = hospitalRepository.findByCity(city);
            packages = hospitals.stream()
                    .flatMap(h -> packageRepository.findByHospitalId(h.getId()).stream())
                    .toList();
        } else if (specialty != null && !specialty.isBlank()) {
            List<HospitalPartner> hospitals = hospitalRepository.findAll().stream()
                    .filter(h -> h.getSpecialties() != null && h.getSpecialties().contains(specialty))
                    .toList();
            packages = hospitals.stream()
                    .flatMap(h -> packageRepository.findByHospitalId(h.getId()).stream())
                    .toList();
        } else {
            packages = packageRepository.findAll(PageRequest.of(0, 100)).getContent();
        }

        return packages.stream().map(this::toDto).toList();
    }

    private MedicalStayPackageDto toDto(MedicalStayPackage pkg) {
        HospitalPartner hospital = hospitalRepository.findById(pkg.getHospitalId()).orElse(null);
        Listing listing = listingRepository.findById(pkg.getListingId()).orElse(null);

        String primaryPhotoUrl = null;
        if (listing != null) {
            var primaryMedia = listingMediaRepository.findFirstByListingIdAndIsPrimaryTrue(listing.getId());
            if (primaryMedia != null) {
                primaryPhotoUrl = primaryMedia.getCdnUrl();
            }
        }

        return new MedicalStayPackageDto(
                pkg.getId(),
                pkg.getListingId(),
                listing != null ? listing.getTitle() : null,
                listing != null ? listing.getCity() : null,
                listing != null ? listing.getState() : null,
                listing != null ? listing.getBasePricePaise() : null,
                primaryPhotoUrl,
                listing != null ? listing.getAmenities() : null,
                pkg.getHospitalId(),
                hospital != null ? hospital.getName() : null,
                hospital != null ? hospital.getCity() : null,
                hospital != null ? (hospital.getSpecialties() != null ? List.of(hospital.getSpecialties().split(",")) : List.of()) : List.of(),
                hospital != null ? (hospital.getAccreditations() != null ? List.of(hospital.getAccreditations().split(",")) : List.of()) : List.of(),
                hospital != null && hospital.getRating() != null ? hospital.getRating().doubleValue() : null,
                pkg.getDistanceKm() != null ? pkg.getDistanceKm().doubleValue() : null,
                pkg.getIncludesPickup(),
                pkg.getIncludesTranslator(),
                pkg.getCaregiverFriendly(),
                pkg.getMedicalPricePaise(),
                pkg.getMinStayNights(),
                pkg.getRecoveryDays()
        );
    }

    @Cacheable(value = "hospitals")
    public Page<HospitalPartner> getHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable);
    }

    public Page<HospitalPartner> getHospitalsByCity(String city, Pageable pageable) {
        if (city == null || city.isBlank()) return hospitalRepository.findAll(pageable);
        return hospitalRepository.findByCity(city, pageable);
    }

    // ── Procedure methods ────────────────────────────────────

    public List<HospitalProcedureDto> getProcedures(UUID hospitalId) {
        return procedureRepository.findByHospitalId(hospitalId).stream()
                .map(this::toProcedureDto).toList();
    }

    public List<HospitalProcedureDto> searchProcedures(String query) {
        return procedureRepository.findByProcedureNameContainingIgnoreCase(query).stream()
                .map(this::toProcedureDto).toList();
    }

    @Cacheable(value = "procedures", key = "#specialty")
    public List<HospitalProcedureDto> getProceduresBySpecialty(String specialty) {
        return procedureRepository.findBySpecialty(specialty).stream()
                .map(this::toProcedureDto).toList();
    }

    @Transactional
    public HospitalProcedureDto addProcedure(UUID hospitalId, HospitalProcedureDto req) {
        hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found"));
        HospitalProcedure proc = HospitalProcedure.builder()
                .hospitalId(hospitalId)
                .procedureName(req.procedureName())
                .specialty(req.specialty())
                .estCostMinPaise(req.estCostMinPaise())
                .estCostMaxPaise(req.estCostMaxPaise())
                .hospitalDays(req.hospitalDays() != null ? req.hospitalDays() : 3)
                .recoveryDays(req.recoveryDays() != null ? req.recoveryDays() : 7)
                .successRate(req.successRate())
                .description(req.description())
                .build();
        return toProcedureDto(procedureRepository.save(proc));
    }

    // ── Cost estimate ────────────────────────────────────────

    public MedicalCostEstimate estimateCost(UUID procedureId, UUID packageId) {
        HospitalProcedure proc = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException("Procedure not found"));
        MedicalStayPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found"));
        HospitalPartner hospital = hospitalRepository.findById(proc.getHospitalId())
                .orElseThrow(() -> new NoSuchElementException("Hospital not found"));

        int stayNights = proc.getRecoveryDays();
        long stayPerNight = pkg.getMedicalPricePaise();
        long stayTotal = stayPerNight * stayNights;
        long totalMin = proc.getEstCostMinPaise() + stayTotal;
        long totalMax = proc.getEstCostMaxPaise() + stayTotal;

        List<String> timeline = new java.util.ArrayList<>();
        timeline.add("Day 1-" + proc.getHospitalDays() + ": Hospital stay (" + hospital.getName() + ")");
        timeline.add("Day " + (proc.getHospitalDays() + 1) + "-" + (proc.getHospitalDays() + proc.getRecoveryDays()) + ": Recovery at nearby stay");
        timeline.add("Day " + (proc.getHospitalDays() + proc.getRecoveryDays() + 1) + ": Follow-up & discharge");

        return new MedicalCostEstimate(
                proc.getProcedureName(), proc.getSpecialty(),
                hospital.getName(), hospital.getCity(),
                proc.getEstCostMinPaise(), proc.getEstCostMaxPaise(),
                proc.getHospitalDays(), proc.getRecoveryDays(),
                stayPerNight, stayNights, stayTotal,
                totalMin, totalMax, timeline
        );
    }

    private HospitalProcedureDto toProcedureDto(HospitalProcedure p) {
        return new HospitalProcedureDto(
                p.getId(), p.getHospitalId(), p.getProcedureName(), p.getSpecialty(),
                p.getEstCostMinPaise(), p.getEstCostMaxPaise(),
                p.getHospitalDays(), p.getRecoveryDays(),
                p.getSuccessRate(), p.getDescription()
        );
    }

    /**
     * Haversine formula to calculate distance in km between two lat/lng points.
     */
    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
