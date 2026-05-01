package com.safar.listing.entity.enums;

/**
 * Top-level kind of a builder project. Determines whether unit_types rows
 * carry BHK + carpet area (UNIT) or plot dimensions (PLOT), and which
 * frontend wizard branch is used.
 */
public enum ProjectType {
    APARTMENT_TOWNSHIP,   // flats / apartments — default
    PLOTTED_DEVELOPMENT,  // bare plots in a layout (RERA / non-RERA)
    VILLA_COMMUNITY       // villas / row houses (built unit, lower density)
}
