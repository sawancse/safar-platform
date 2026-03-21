package com.safar.listing.service;

import com.safar.listing.dto.MedicalPackageRequest;
import com.safar.listing.entity.HospitalPartner;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.MedicalStayPackage;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.PricingUnit;
import com.safar.listing.repository.HospitalPartnerRepository;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.MedicalStayPackageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalStayServiceTest {

    @Mock HospitalPartnerRepository hospitalRepository;
    @Mock MedicalStayPackageRepository packageRepository;
    @Mock ListingRepository listingRepository;

    @InjectMocks MedicalStayService medicalStayService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();
    private final UUID hospitalId = UUID.randomUUID();

    /**
     * Test 1: Haversine distance calculation — verify known straight-line distance.
     * Mumbai (19.0760, 72.8777) to Pune (18.5204, 73.8567): ~120 km straight line
     */
    @Test
    void haversineKm_mumbaiToPune_calculatesCorrectly() {
        double distance = MedicalStayService.haversineKm(
                19.0760, 72.8777,  // Mumbai
                18.5204, 73.8567   // Pune
        );

        // Haversine straight-line distance is approximately 120 km
        assertThat(distance).isBetween(118.0, 123.0);
    }

    /**
     * Test 2: Registering a package sets listing.medicalStay = true.
     */
    @Test
    void registerPackage_setsListingMedicalStayTrue() {
        Listing listing = Listing.builder()
                .id(listingId)
                .hostId(hostId)
                .title("Mumbai Stay")
                .description("Near hospital")
                .type(ListingType.HOME)
                .addressLine1("123 Main St")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .lat(BigDecimal.valueOf(19.0760))
                .lng(BigDecimal.valueOf(72.8777))
                .maxGuests(4)
                .basePricePaise(500_000L)
                .pricingUnit(PricingUnit.NIGHT)
                .medicalStay(false)
                .build();

        HospitalPartner hospital = HospitalPartner.builder()
                .id(hospitalId)
                .name("Apollo Hospital")
                .city("Mumbai")
                .address("Navi Mumbai")
                .lat(BigDecimal.valueOf(19.0330))
                .lng(BigDecimal.valueOf(73.0297))
                .specialties("cardiology,orthopedics")
                .accreditations("NABH,JCI")
                .active(true)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(packageRepository.save(any(MedicalStayPackage.class))).thenAnswer(inv -> {
            MedicalStayPackage pkg = inv.getArgument(0);
            pkg.setId(UUID.randomUUID());
            return pkg;
        });
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalPackageRequest req = new MedicalPackageRequest(
                hospitalId, true, true, false, 200_000L, 5);

        MedicalStayPackage result = medicalStayService.registerPackage(hostId, listingId, req);

        assertThat(listing.getMedicalStay()).isTrue();
        assertThat(result.getIncludesPickup()).isTrue();
        assertThat(result.getIncludesTranslator()).isTrue();
        assertThat(result.getMedicalPricePaise()).isEqualTo(200_000L);
        assertThat(result.getDistanceKm()).isNotNull();

        verify(listingRepository).save(listing);
    }
}
