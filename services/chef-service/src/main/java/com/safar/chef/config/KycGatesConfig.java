package com.safar.chef.config;

import com.safar.chef.entity.enums.KycDocumentType;
import com.safar.chef.entity.enums.ServiceListingType;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-service-type KYC requirements (Practo "Bluebook" pattern).
 *
 * A vendor cannot transition DRAFT -> PENDING_REVIEW unless every required
 * document for their service type is uploaded. FSSAI is legally mandatory for
 * any e-commerce food sale (6-month imprisonment + ₹5L fine for violation),
 * so cake/cook gates are non-negotiable.
 *
 * Stored as static config (not DB) so changes are reviewable in code review,
 * not silently mutable by admins.
 */
@Configuration
public class KycGatesConfig {

    private static final Map<ServiceListingType, Set<KycDocumentType>> REQUIRED;
    private static final Map<ServiceListingType, Set<KycDocumentType>> OPTIONAL;

    static {
        REQUIRED = new EnumMap<>(ServiceListingType.class);
        OPTIONAL = new EnumMap<>(ServiceListingType.class);

        // CAKE_DESIGNER — FSSAI legally mandatory
        REQUIRED.put(ServiceListingType.CAKE_DESIGNER, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN, KycDocumentType.FSSAI));
        OPTIONAL.put(ServiceListingType.CAKE_DESIGNER, Set.of(KycDocumentType.GST));

        // COOK — same as cake (food sale)
        REQUIRED.put(ServiceListingType.COOK, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN, KycDocumentType.FSSAI));
        OPTIONAL.put(ServiceListingType.COOK, Set.of(KycDocumentType.GST));

        // SINGER — no statutory gate
        REQUIRED.put(ServiceListingType.SINGER, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN));
        OPTIONAL.put(ServiceListingType.SINGER, Set.of(KycDocumentType.GST, KycDocumentType.IPRS));

        // PANDIT — lineage proof mandatory
        REQUIRED.put(ServiceListingType.PANDIT, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN, KycDocumentType.LINEAGE_PROOF));
        OPTIONAL.put(ServiceListingType.PANDIT, Set.of());

        // DECORATOR — no statutory gate (insurance optional)
        REQUIRED.put(ServiceListingType.DECORATOR, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN));
        OPTIONAL.put(ServiceListingType.DECORATOR, Set.of(KycDocumentType.GST, KycDocumentType.INSURANCE));

        // STAFF_HIRE — police verification mandatory (entering customer homes)
        REQUIRED.put(ServiceListingType.STAFF_HIRE, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN, KycDocumentType.POLICE_VERIFICATION));
        OPTIONAL.put(ServiceListingType.STAFF_HIRE, Set.of(KycDocumentType.GST));

        // APPLIANCE_RENTAL — GST mandatory (commercial sale)
        REQUIRED.put(ServiceListingType.APPLIANCE_RENTAL, Set.of(
                KycDocumentType.AADHAAR, KycDocumentType.PAN, KycDocumentType.GST));
        OPTIONAL.put(ServiceListingType.APPLIANCE_RENTAL, Set.of());

        // V2 types — minimal gates until each launches
        for (ServiceListingType v2 : new ServiceListingType[]{
                ServiceListingType.PHOTOGRAPHER, ServiceListingType.DJ,
                ServiceListingType.MEHENDI, ServiceListingType.MAKEUP_ARTIST}) {
            REQUIRED.put(v2, Set.of(KycDocumentType.AADHAAR, KycDocumentType.PAN));
            OPTIONAL.put(v2, Set.of(KycDocumentType.GST));
        }
    }

    public Set<KycDocumentType> requiredFor(ServiceListingType type) {
        return REQUIRED.getOrDefault(type, Set.of());
    }

    public Set<KycDocumentType> optionalFor(ServiceListingType type) {
        return OPTIONAL.getOrDefault(type, Set.of());
    }
}
