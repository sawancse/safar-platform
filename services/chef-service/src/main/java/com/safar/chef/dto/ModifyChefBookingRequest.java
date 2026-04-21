package com.safar.chef.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ModifyChefBookingRequest(
    LocalDate serviceDate,
    String serviceTime,
    Integer guestsCount,
    Integer numberOfMeals,
    UUID menuId,
    String specialRequests,
    String address,
    String city,
    String locality,
    String pincode,
    String appliancesJson,
    String crockeryJson
) {}
