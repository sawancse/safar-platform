package com.safar.services.dto;

import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.ServiceListingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public/private projection of a ServiceListing parent. Type-specific fields
 * (cake/singer/pandit/...) are exposed via separate per-type endpoints when
 * the wizard or storefront needs them.
 */
public record ServiceListingResponse(
        UUID id,
        UUID vendorUserId,
        String serviceType,
        String businessName,
        String vendorSlug,
        String heroImageUrl,
        String tagline,
        String aboutMd,
        Integer foundedYear,
        ServiceListingStatus status,
        String rejectionReason,
        List<String> cities,
        String homeCity,
        String homePincode,
        BigDecimal homeLat,
        BigDecimal homeLng,
        Integer deliveryRadiusKm,
        Boolean outstationCapable,
        List<String> deliveryChannels,
        String pricingPattern,
        String pricingFormula,
        String calendarMode,
        Integer defaultLeadTimeHours,
        String cancellationPolicy,
        BigDecimal avgRating,
        Integer ratingCount,
        Integer completedBookingsCount,
        String trustTier,
        String commissionTier,
        BigDecimal commissionPctOverride,
        String subscriptionPlan,
        OffsetDateTime subscriptionExpiresAt,
        Boolean hasPendingChanges,
        String pendingChanges,
        OffsetDateTime pendingChangesSubmittedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceListingResponse from(ServiceListing l) {
        return new ServiceListingResponse(
                l.getId(),
                l.getVendorUserId(),
                l.getServiceType(),
                l.getBusinessName(),
                l.getVendorSlug(),
                l.getHeroImageUrl(),
                l.getTagline(),
                l.getAboutMd(),
                l.getFoundedYear(),
                l.getStatus(),
                l.getRejectionReason(),
                l.getCities(),
                l.getHomeCity(),
                l.getHomePincode(),
                l.getHomeLat(),
                l.getHomeLng(),
                l.getDeliveryRadiusKm(),
                l.getOutstationCapable(),
                l.getDeliveryChannels(),
                l.getPricingPattern(),
                l.getPricingFormula(),
                l.getCalendarMode(),
                l.getDefaultLeadTimeHours(),
                l.getCancellationPolicy(),
                l.getAvgRating(),
                l.getRatingCount(),
                l.getCompletedBookingsCount(),
                l.getTrustTier(),
                l.getCommissionTier(),
                l.getCommissionPctOverride(),
                l.getSubscriptionPlan(),
                l.getSubscriptionExpiresAt(),
                l.getHasPendingChanges(),
                l.getPendingChanges(),
                l.getPendingChangesSubmittedAt(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }
}
