package com.safar.chef.repository;

import com.safar.chef.entity.VendorKycDocument;
import com.safar.chef.entity.enums.KycDocumentType;
import com.safar.chef.entity.enums.KycVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorKycDocumentRepository extends JpaRepository<VendorKycDocument, UUID> {

    List<VendorKycDocument> findByServiceListingId(UUID serviceListingId);

    List<VendorKycDocument> findByServiceListingIdAndDocumentType(UUID serviceListingId, KycDocumentType documentType);

    List<VendorKycDocument> findByServiceListingIdAndVerificationStatus(UUID serviceListingId, KycVerificationStatus status);

    boolean existsByServiceListingIdAndDocumentType(UUID serviceListingId, KycDocumentType documentType);
}
