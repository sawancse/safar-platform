package com.safar.listing.entity.enums;

/**
 * Discriminator on project_unit_types — UNIT carries BHK + carpet area
 * (apartments / villas), PLOT carries plot dimensions and per-sqft price.
 */
public enum UnitKind {
    UNIT,   // built apartment / villa unit
    PLOT    // bare plot in a plotted development
}
