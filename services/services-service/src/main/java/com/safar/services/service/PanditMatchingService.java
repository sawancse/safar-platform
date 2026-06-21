package com.safar.services.service;

import com.safar.services.dto.MatchPanditsRequest;
import com.safar.services.dto.MatchedPanditResponse;
import com.safar.services.entity.PanditAttributes;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.ServiceListingStatus;
import com.safar.services.repository.ServiceListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Scores VERIFIED pandit listings against a customer's puja criteria and returns
 * them best-first. Read-only, no persistence — replaces today's manual admin
 * assignment with a ranked shortlist (gotra / tradition / language / puja-type /
 * city aware). Mirrors the dish-based chef matcher but for the pandit vertical.
 */
@Service
@RequiredArgsConstructor
public class PanditMatchingService {

    private final ServiceListingRepository listingRepo;

    /** Loose occasion -> candidate puja-type mapping, used when the caller
     *  doesn't pass an explicit pujaType (e.g. matching straight from an occasion). */
    private static final Map<String, List<String>> OCCASION_PUJAS = Map.of(
            "HOUSEWARMING", List.of("GRIHA_PRAVESH", "VASTU_SHANTI", "BHOOMI_PUJAN"),
            "MARRIAGE",     List.of("WEDDING", "ENGAGEMENT"),
            "BABY",         List.of("NAMKARAN", "ANNAPRASHAN", "MUNDAN"),
            "ANNIVERSARY",  List.of("SATYANARAYAN"),
            "DOSH_NIVARAN", List.of("KAAL_SARP", "MANGAL_DOSH", "PITRA_DOSH", "SHANI_SHANTI", "NAVAGRAHA"),
            "PAATH_JAAP",   List.of("SUNDARKAND", "MAHA_MRITYUNJAYA", "RUDRABHISHEK"),
            "FESTIVAL",     List.of("LAKSHMI_PUJA", "GANESH_PUJA"),
            "CAR_VEHICLE",  List.of("VEHICLE_PUJA"));

    public List<MatchedPanditResponse> match(MatchPanditsRequest req) {
        List<ServiceListing> verified =
                listingRepo.findByServiceTypeAndStatus("PANDIT", ServiceListingStatus.VERIFIED);

        List<MatchedPanditResponse> scored = new ArrayList<>();
        for (ServiceListing listing : verified) {
            if (!(listing instanceof PanditAttributes p)) continue;   // defensive: only pandit children
            scored.add(score(p, req));
        }

        scored.sort(Comparator.comparingInt(MatchedPanditResponse::matchScore).reversed());
        return scored.size() > 12 ? scored.subList(0, 12) : scored;
    }

    private MatchedPanditResponse score(PanditAttributes p, MatchPanditsRequest req) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // ── Language ───────────────────────────────────────────
        if (has(req.language()) && containsIgnoreCase(p.getTextLanguages(), req.language())) {
            score += 30; reasons.add("Speaks " + req.language());
        }

        // ── Puja type (explicit, else derived from occasion) ───
        List<String> wantedPujas = new ArrayList<>();
        if (has(req.pujaType())) wantedPujas.add(req.pujaType());
        else if (has(req.occasion()))
            wantedPujas.addAll(OCCASION_PUJAS.getOrDefault(req.occasion().toUpperCase(), List.of()));
        for (String puja : wantedPujas) {
            if (containsIgnoreCase(p.getPujaTypesOffered(), puja)) {
                score += has(req.pujaType()) ? 30 : 18;
                reasons.add("Performs " + prettify(puja));
                break;
            }
        }

        // ── Tradition ──────────────────────────────────────────
        if (has(req.tradition()) && eqIgnoreCase(p.getTradition(), req.tradition())) {
            score += 15; reasons.add(prettify(req.tradition()) + " tradition");
        }

        // ── Gotra ──────────────────────────────────────────────
        if (has(req.gotra()) && eqIgnoreCase(p.getPanditGotra(), req.gotra())) {
            score += 10; reasons.add("Same gotra (" + req.gotra() + ")");
        }

        // ── City ───────────────────────────────────────────────
        if (has(req.city()) && eqIgnoreCase(p.getHomeCity(), req.city())) {
            score += 20; reasons.add("Based in " + req.city());
        }

        // ── Samagri ────────────────────────────────────────────
        if ("ALL".equalsIgnoreCase(p.getSamagriProvided())) {
            score += 10; reasons.add("Brings all samagri");
        }

        // ── Online puja ────────────────────────────────────────
        if (Boolean.TRUE.equals(req.onlineOk()) && Boolean.TRUE.equals(p.getOnlineViaVideoCall())) {
            score += 5; reasons.add("Can perform online");
        }

        // ── Quality / trust weighting (tie-breakers) ───────────
        Number avg = p.getAvgRating();
        double rating = avg != null ? avg.doubleValue() : 0.0;
        score += (int) Math.round(rating * 4);                       // up to ~20
        String tier = p.getTrustTier();
        if ("TOP_RATED".equals(tier))        { score += 15; reasons.add("Top rated"); }
        else if ("SAFAR_VERIFIED".equals(tier)) { score += 8; }
        Integer completed = p.getCompletedBookingsCount();
        if (completed != null) score += Math.min(completed, 10);

        return new MatchedPanditResponse(
                p.getId(),
                p.getBusinessName(),
                p.getVendorSlug(),
                p.getHeroImageUrl(),
                p.getHomeCity(),
                rating,
                p.getRatingCount(),
                completed,
                tier,
                p.getTradition(),
                p.getTextLanguages(),
                p.getPujaTypesOffered(),
                p.getSamagriProvided(),
                p.getOnlineViaVideoCall(),
                score,
                reasons);
    }

    // ── helpers ───────────────────────────────────────────────
    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static boolean eqIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean containsIgnoreCase(List<String> list, String val) {
        if (list == null || val == null) return false;
        return list.stream().anyMatch(v -> v != null && v.equalsIgnoreCase(val));
    }

    /** GRIHA_PRAVESH -> "Griha Pravesh". */
    private static String prettify(String token) {
        if (token == null) return null;
        String[] parts = token.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
