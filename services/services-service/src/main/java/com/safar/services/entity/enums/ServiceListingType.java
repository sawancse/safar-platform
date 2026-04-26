package com.safar.services.entity.enums;

/**
 * Service-leg provider types. Discriminator on services.service_listings.
 *
 * MVP types: CAKE_DESIGNER, SINGER, PANDIT, DECORATOR, STAFF_HIRE.
 * Migrated from chef-service: COOK (formerly chef profiles).
 * MVP-conditional: APPLIANCE_RENTAL.
 * V2: PHOTOGRAPHER, DJ, MEHENDI, MAKEUP_ARTIST.
 *
 * Distinct from {@link VendorServiceType} (which is the legacy partner_vendors
 * directory enum — to be retired in V25 once backfill completes).
 */
public enum ServiceListingType {
    CAKE_DESIGNER,
    SINGER,
    PANDIT,
    DECORATOR,
    STAFF_HIRE,
    COOK,
    APPLIANCE_RENTAL,
    PHOTOGRAPHER,
    DJ,
    MEHENDI,
    MAKEUP_ARTIST
}
