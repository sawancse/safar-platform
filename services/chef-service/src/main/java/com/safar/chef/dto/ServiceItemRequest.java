package com.safar.chef.dto;

import java.util.List;
import java.util.Map;

public record ServiceItemRequest(
        String title,
        String heroPhotoUrl,
        List<String> photos,
        String descriptionMd,
        Long basePricePaise,
        Map<String, Object> options,                  // serialized to options_json JSONB
        List<String> occasionTags,
        Integer leadTimeHours,
        Integer displayOrder
) {}
