package com.safar.services.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.services.dto.ServiceItemRequest;
import com.safar.services.entity.ServiceItem;
import com.safar.services.entity.ServiceListing;
import com.safar.services.repository.ServiceItemRepository;
import com.safar.services.repository.ServiceListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for service_items (Pattern B/D — vendor publishes items inside their
 * listing storefront). Booking row stores service_item_id so the customer
 * dashboard deep-links to "the cake/puja/singer you ordered" — Goal B.
 *
 * Item types are catalog-driven only: cake/decor/pandit/appliance. For
 * singer/staff/cook, vendor IS the item — items are optional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceItemService {

    private final ServiceItemRepository itemRepo;
    private final ServiceListingRepository listingRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ServiceItem> listForListing(UUID listingId, boolean activeOnly) {
        if (activeOnly) {
            return itemRepo.findByServiceListingIdAndStatusOrderByDisplayOrderAsc(listingId, "ACTIVE");
        }
        return itemRepo.findByServiceListingId(listingId);
    }

    @Transactional(readOnly = true)
    public ServiceItem get(UUID itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Service item not found: " + itemId));
    }

    @Transactional
    public ServiceItem create(UUID listingId, ServiceItemRequest req, UUID vendorUserId) {
        ServiceListing listing = mustOwn(listingId, vendorUserId);
        validate(req);
        ServiceItem item = ServiceItem.builder()
                .serviceListingId(listing.getId())
                .title(req.title())
                .heroPhotoUrl(req.heroPhotoUrl())
                .photos(req.photos())
                .descriptionMd(req.descriptionMd())
                .basePricePaise(req.basePricePaise())
                .optionsJson(toJson(req.options()))
                .occasionTags(req.occasionTags())
                .leadTimeHours(req.leadTimeHours())
                .displayOrder(req.displayOrder() == null ? 0 : req.displayOrder())
                .status("ACTIVE")
                .build();
        ServiceItem saved = itemRepo.save(item);
        log.info("Service item {} created for listing {}", saved.getId(), listing.getId());
        return saved;
    }

    @Transactional
    public ServiceItem update(UUID itemId, ServiceItemRequest req, UUID vendorUserId) {
        ServiceItem item = get(itemId);
        ServiceListing listing = listingRepo.findById(item.getServiceListingId())
                .orElseThrow();
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Item belongs to another vendor's listing");
        }
        validate(req);

        if (req.title() != null) item.setTitle(req.title());
        if (req.heroPhotoUrl() != null) item.setHeroPhotoUrl(req.heroPhotoUrl());
        if (req.photos() != null) item.setPhotos(req.photos());
        if (req.descriptionMd() != null) item.setDescriptionMd(req.descriptionMd());
        if (req.basePricePaise() != null) item.setBasePricePaise(req.basePricePaise());
        if (req.options() != null) item.setOptionsJson(toJson(req.options()));
        if (req.occasionTags() != null) item.setOccasionTags(req.occasionTags());
        if (req.leadTimeHours() != null) item.setLeadTimeHours(req.leadTimeHours());
        if (req.displayOrder() != null) item.setDisplayOrder(req.displayOrder());

        return itemRepo.save(item);
    }

    @Transactional
    public ServiceItem setStatus(UUID itemId, String newStatus, UUID vendorUserId) {
        if (!"ACTIVE".equals(newStatus) && !"PAUSED".equals(newStatus)) {
            throw new IllegalArgumentException("status must be ACTIVE or PAUSED");
        }
        ServiceItem item = get(itemId);
        ServiceListing listing = listingRepo.findById(item.getServiceListingId()).orElseThrow();
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Item belongs to another vendor's listing");
        }
        item.setStatus(newStatus);
        return itemRepo.save(item);
    }

    @Transactional
    public void delete(UUID itemId, UUID vendorUserId) {
        ServiceItem item = get(itemId);
        ServiceListing listing = listingRepo.findById(item.getServiceListingId()).orElseThrow();
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Item belongs to another vendor's listing");
        }
        itemRepo.deleteById(itemId);
        log.info("Service item {} deleted by vendor {}", itemId, vendorUserId);
    }

    private ServiceListing mustOwn(UUID listingId, UUID vendorUserId) {
        ServiceListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        if (!listing.getVendorUserId().equals(vendorUserId)) {
            throw new AccessDeniedException("Listing belongs to another vendor");
        }
        return listing;
    }

    private void validate(ServiceItemRequest req) {
        if (req.title() == null || req.title().isBlank())
            throw new IllegalArgumentException("title required");
        if (req.basePricePaise() == null || req.basePricePaise() < 0)
            throw new IllegalArgumentException("basePricePaise required and non-negative");
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid options json: " + e.getMessage()); }
    }
}
