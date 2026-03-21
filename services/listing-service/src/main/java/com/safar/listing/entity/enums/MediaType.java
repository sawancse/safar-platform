package com.safar.listing.entity.enums;

/**
 * Media type for listing media.
 * Note: The DB also allows '3D' but that is not a valid Java identifier.
 * '3D' media is not exposed via the API in this version.
 */
public enum MediaType {
    PHOTO, VIDEO, PANORAMA, FLOOR_PLAN, VIDEO_TOUR, DRONE
}
