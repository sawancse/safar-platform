package com.safar.chef.service;

import com.safar.chef.config.KycGatesConfig;
import com.safar.chef.entity.ServiceListing;
import com.safar.chef.entity.VendorKycDocument;
import com.safar.chef.entity.enums.KycDocumentType;
import com.safar.chef.entity.enums.KycVerificationStatus;
import com.safar.chef.entity.enums.ServiceListingType;
import com.safar.chef.repository.VendorKycDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceListingPublishValidatorTest {

    @Mock VendorKycDocumentRepository kycRepo;

    @Spy KycGatesConfig kycGates = new KycGatesConfig();

    @InjectMocks ServiceListingPublishValidator validator;

    private ServiceListing newListing(ServiceListingType type) {
        ServiceListing l = new ServiceListing();
        l.setId(UUID.randomUUID());
        l.setVendorUserId(UUID.randomUUID());
        l.setBusinessName("Test Vendor");
        l.setVendorSlug("test-vendor");
        // Hibernate normally sets serviceType from the discriminator on the
        // child entity, but for unit tests we set it manually since we're
        // bypassing JPA. The validator reads it as a String and parses to enum.
        l.setServiceType(type.name());
        return l;
    }

    private VendorKycDocument doc(UUID listingId, KycDocumentType type, KycVerificationStatus status) {
        return VendorKycDocument.builder()
                .serviceListingId(listingId)
                .documentType(type)
                .documentUrl("https://example.com/doc.pdf")
                .verificationStatus(status)
                .build();
    }

    @Test
    void cake_designer_passes_when_aadhaar_pan_fssai_uploaded() {
        ServiceListing l = newListing(ServiceListingType.CAKE_DESIGNER);
        when(kycRepo.findByServiceListingId(l.getId())).thenReturn(List.of(
                doc(l.getId(), KycDocumentType.AADHAAR, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.PAN, KycVerificationStatus.VERIFIED),
                doc(l.getId(), KycDocumentType.FSSAI, KycVerificationStatus.PENDING)
        ));

        validator.validateOrThrow(l);  // should not throw
    }

    @Test
    void cake_designer_fails_when_fssai_missing() {
        ServiceListing l = newListing(ServiceListingType.CAKE_DESIGNER);
        when(kycRepo.findByServiceListingId(l.getId())).thenReturn(List.of(
                doc(l.getId(), KycDocumentType.AADHAAR, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.PAN, KycVerificationStatus.PENDING)
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(l))
                .isInstanceOf(ServiceListingPublishValidator.MissingKycException.class)
                .satisfies(ex -> {
                    var missing = ((ServiceListingPublishValidator.MissingKycException) ex).getMissing();
                    assertThat(missing).contains(KycDocumentType.FSSAI);
                });
    }

    @Test
    void cake_designer_fails_when_fssai_rejected() {
        ServiceListing l = newListing(ServiceListingType.CAKE_DESIGNER);
        when(kycRepo.findByServiceListingId(l.getId())).thenReturn(List.of(
                doc(l.getId(), KycDocumentType.AADHAAR, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.PAN, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.FSSAI, KycVerificationStatus.REJECTED)
        ));

        // REJECTED docs don't satisfy the gate — only PENDING or VERIFIED count.
        assertThatThrownBy(() -> validator.validateOrThrow(l))
                .isInstanceOf(ServiceListingPublishValidator.MissingKycException.class);
    }

    @Test
    void staff_hire_requires_police_verification() {
        ServiceListing l = newListing(ServiceListingType.STAFF_HIRE);
        when(kycRepo.findByServiceListingId(l.getId())).thenReturn(List.of(
                doc(l.getId(), KycDocumentType.AADHAAR, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.PAN, KycVerificationStatus.PENDING)
        ));

        assertThatThrownBy(() -> validator.validateOrThrow(l))
                .isInstanceOf(ServiceListingPublishValidator.MissingKycException.class)
                .satisfies(ex -> {
                    var missing = ((ServiceListingPublishValidator.MissingKycException) ex).getMissing();
                    assertThat(missing).contains(KycDocumentType.POLICE_VERIFICATION);
                });
    }

    @Test
    void singer_passes_with_aadhaar_pan_only_no_iprs_required() {
        ServiceListing l = newListing(ServiceListingType.SINGER);
        when(kycRepo.findByServiceListingId(l.getId())).thenReturn(List.of(
                doc(l.getId(), KycDocumentType.AADHAAR, KycVerificationStatus.PENDING),
                doc(l.getId(), KycDocumentType.PAN, KycVerificationStatus.PENDING)
        ));

        validator.validateOrThrow(l);  // should not throw — IPRS is optional for singer
    }

    @Test
    void unknown_service_type_throws_clear_error() {
        ServiceListing l = newListing(ServiceListingType.SINGER);
        l.setServiceType("MARTIAN_DJ");  // bogus type

        assertThatThrownBy(() -> validator.validateOrThrow(l))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown service_type");
    }
}
