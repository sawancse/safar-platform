package com.safar.chef.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.chef.entity.ServiceItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ServiceItemResponse(
        UUID id,
        UUID serviceListingId,
        String title,
        String heroPhotoUrl,
        List<String> photos,
        String descriptionMd,
        Long basePricePaise,
        Map<String, Object> options,
        List<String> occasionTags,
        Integer leadTimeHours,
        String status,
        Integer displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    @SuppressWarnings("unchecked")
    public static ServiceItemResponse from(ServiceItem i, ObjectMapper mapper) {
        Map<String, Object> opts = null;
        if (i.getOptionsJson() != null && !i.getOptionsJson().isBlank()) {
            try { opts = mapper.readValue(i.getOptionsJson(), Map.class); }
            catch (Exception ignored) { /* leave as null */ }
        }
        return new ServiceItemResponse(
                i.getId(), i.getServiceListingId(), i.getTitle(), i.getHeroPhotoUrl(),
                i.getPhotos(), i.getDescriptionMd(), i.getBasePricePaise(),
                opts, i.getOccasionTags(), i.getLeadTimeHours(),
                i.getStatus(), i.getDisplayOrder(),
                i.getCreatedAt(), i.getUpdatedAt()
        );
    }
}
