package com.safar.chef.dto;

import java.util.List;
import java.util.UUID;

public record AddDishOfferingsRequest(
        List<UUID> dishIds,
        Long customPricePaise
) {}
