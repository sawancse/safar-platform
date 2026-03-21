package com.safar.listing.service;

import com.safar.listing.dto.PgPackageRequest;
import com.safar.listing.dto.PgPackageResponse;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.PgPackage;
import com.safar.listing.repository.ListingRepository;
import com.safar.listing.repository.PgPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PgPackageService {

    private final PgPackageRepository packageRepo;
    private final ListingRepository listingRepo;

    @Transactional
    public PgPackageResponse createPackage(UUID listingId, UUID hostId, PgPackageRequest req) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        int nextSort = packageRepo.findByListingIdOrderBySortOrder(listingId).size();

        PgPackage pkg = PgPackage.builder()
                .listingId(listingId)
                .name(req.name())
                .description(req.description())
                .monthlyPricePaise(req.monthlyPricePaise())
                .includesMeals(req.includesMeals() != null ? req.includesMeals() : false)
                .includesLaundry(req.includesLaundry() != null ? req.includesLaundry() : false)
                .includesWifi(req.includesWifi() != null ? req.includesWifi() : false)
                .includesHousekeeping(req.includesHousekeeping() != null ? req.includesHousekeeping() : false)
                .sortOrder(nextSort)
                .build();

        PgPackage saved = packageRepo.save(pkg);
        log.info("Created PG package '{}' for listing {}", saved.getName(), listingId);
        return toResponse(saved);
    }

    @Transactional
    public PgPackageResponse updatePackage(UUID listingId, UUID packageId, UUID hostId, PgPackageRequest req) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        PgPackage pkg = packageRepo.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found: " + packageId));
        if (!pkg.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Package does not belong to this listing");
        }

        pkg.setName(req.name());
        pkg.setDescription(req.description());
        pkg.setMonthlyPricePaise(req.monthlyPricePaise());
        pkg.setIncludesMeals(req.includesMeals() != null ? req.includesMeals() : false);
        pkg.setIncludesLaundry(req.includesLaundry() != null ? req.includesLaundry() : false);
        pkg.setIncludesWifi(req.includesWifi() != null ? req.includesWifi() : false);
        pkg.setIncludesHousekeeping(req.includesHousekeeping() != null ? req.includesHousekeeping() : false);

        PgPackage updated = packageRepo.save(pkg);
        log.info("Updated PG package '{}' for listing {}", updated.getName(), listingId);
        return toResponse(updated);
    }

    @Transactional
    public void deletePackage(UUID listingId, UUID packageId, UUID hostId) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        PgPackage pkg = packageRepo.findById(packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found: " + packageId));
        if (!pkg.getListingId().equals(listingId)) {
            throw new IllegalArgumentException("Package does not belong to this listing");
        }

        pkg.setIsActive(false);
        packageRepo.save(pkg);
        log.info("Deactivated PG package {} from listing {}", packageId, listingId);
    }

    public List<PgPackageResponse> getPackages(UUID listingId) {
        return packageRepo.findByListingIdAndIsActiveTrueOrderBySortOrder(listingId)
                .stream().map(this::toResponse).toList();
    }

    public List<PgPackageResponse> getAllPackages(UUID listingId) {
        return packageRepo.findByListingIdOrderBySortOrder(listingId)
                .stream().map(this::toResponse).toList();
    }

    private PgPackageResponse toResponse(PgPackage p) {
        return new PgPackageResponse(
                p.getId(), p.getListingId(), p.getName(), p.getDescription(),
                p.getMonthlyPricePaise(), p.getIncludesMeals(), p.getIncludesLaundry(),
                p.getIncludesWifi(), p.getIncludesHousekeeping(),
                p.getSortOrder(), p.getIsActive()
        );
    }
}
