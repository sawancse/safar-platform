package com.safar.supply.entity.enums;

/**
 * Discriminator for which {@code SupplierAdapter} handles a supplier.
 * MANUAL = admin-driven (no external API). All others = real B2B integrations.
 */
public enum IntegrationType {
    MANUAL,
    UDAAN,
    FNP,
    AMAZON_BUSINESS,
    METRO_CASH_CARRY,
    JUMBOTAIL,
    NINJACART
}
