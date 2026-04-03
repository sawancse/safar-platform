package com.safar.listing.repository;

import com.safar.listing.entity.PropertyInquiry;
import com.safar.listing.entity.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyInquiryRepository extends JpaRepository<PropertyInquiry, UUID> {

    Page<PropertyInquiry> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    Page<PropertyInquiry> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<PropertyInquiry> findBySalePropertyIdOrderByCreatedAtDesc(UUID salePropertyId, Pageable pageable);

    List<PropertyInquiry> findBySellerIdAndStatus(UUID sellerId, InquiryStatus status);

    long countBySalePropertyId(UUID salePropertyId);

    long countBySellerIdAndStatus(UUID sellerId, InquiryStatus status);

    boolean existsByBuyerIdAndSalePropertyId(UUID buyerId, UUID salePropertyId);
}
