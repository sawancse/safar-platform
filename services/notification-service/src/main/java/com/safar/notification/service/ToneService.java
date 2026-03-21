package com.safar.notification.service;

import com.safar.notification.entity.EmailPreference;
import com.safar.notification.repository.EmailPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class ToneService {

    private static final Set<String> CASUAL_TYPES = Set.of("HOSTEL", "HOSTEL_DORM", "PG", "COLIVING", "BUDGET_HOTEL");
    private static final Set<String> FORMAL_TYPES = Set.of("VILLA", "RESORT", "COMMERCIAL");

    private final EmailPreferenceRepository emailPreferenceRepository;

    public ToneService(EmailPreferenceRepository emailPreferenceRepository) {
        this.emailPreferenceRepository = emailPreferenceRepository;
    }

    public String determineTone(UUID userId, String listingType, String discoveryCategories, boolean isMedical) {
        // Check user's explicit preference first
        EmailPreference pref = emailPreferenceRepository.findByUserId(userId).orElse(null);
        if (pref != null && !"AUTO".equals(pref.getTonePreference())) {
            return pref.getTonePreference();
        }

        // Medical stays are always formal
        if (isMedical) return "FORMAL";

        // Check listing type
        if (listingType != null) {
            if (CASUAL_TYPES.contains(listingType.toUpperCase())) return "CASUAL";
            if (FORMAL_TYPES.contains(listingType.toUpperCase())) return "FORMAL";
        }

        // Check discovery categories for heritage/luxury indicators
        if (discoveryCategories != null) {
            String cats = discoveryCategories.toUpperCase();
            if (cats.contains("HERITAGE") || cats.contains("LUXURY") || cats.contains("HAVELI")) return "FORMAL";
            if (cats.contains("BACKPACKER") || cats.contains("BUDGET")) return "CASUAL";
        }

        // Default to formal
        return "FORMAL";
    }
}
