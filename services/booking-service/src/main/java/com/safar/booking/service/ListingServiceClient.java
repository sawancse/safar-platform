package com.safar.booking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ListingServiceClient {
    boolean isAvailable(UUID listingId, LocalDateTime checkIn, LocalDateTime checkOut);
    long getBasePricePaise(UUID listingId);
    UUID getHostId(UUID listingId);
    List<UUID> getGroupListingIds(UUID groupId);
    int getGroupBundleDiscountPct(UUID groupId);
    String getCity(UUID listingId);

    String getListingTitle(UUID listingId);
    default String getListingPhotoUrl(UUID listingId) { return null; }
    default String getListingAddress(UUID listingId) { return null; }
    default String getHostName(UUID listingId) { return null; }
    long getCleaningFeePaise(UUID listingId);
    String getHostTier(UUID hostId);

    // Availability management
    void blockDates(UUID listingId, LocalDate from, LocalDate to);
    void unblockDates(UUID listingId, LocalDate from, LocalDate to);

    // Room type methods
    long getRoomTypePrice(UUID listingId, UUID roomTypeId);
    String getRoomTypeName(UUID listingId, UUID roomTypeId);
    void decrementRoomTypeAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count);
    void incrementRoomTypeAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count);

    // PG/Hotel listing type support
    String getListingType(UUID listingId);
    Integer getNoticePeriodDays(UUID listingId);
    Long getSecurityDepositPaise(UUID listingId);

    // Validation methods (new)
    Map<String, Object> checkAvailability(UUID listingId, LocalDate checkIn, LocalDate checkOut);
    Map<String, Object> checkRoomTypeAvailability(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);
    Integer getMaxGuests(UUID listingId);
    Integer getTotalRooms(UUID listingId);
    Boolean getPetFriendly(UUID listingId);
    Integer getMaxPets(UUID listingId);
    Integer getRoomTypeMaxGuests(UUID roomTypeId);

    // Pricing unit (NIGHT, HOUR, MONTH)
    default String getPricingUnit(UUID listingId) { return "NIGHT"; }

    // Non-refundable & Pay-at-Property
    default int getNonRefundableDiscountPercent(UUID listingId) { return 10; }
    default int getPartialPrepaidPercent(UUID listingId) { return 30; }

    // Room type inclusions
    List<Map<String, Object>> getRoomTypeInclusions(UUID roomTypeId);

    // Room type info (sharingType, count, totalBeds etc.)
    default Map<String, Object> getRoomTypeInfo(UUID roomTypeId) { return Map.of(); }
}
