package com.safar.insurance.adapter;

import com.safar.insurance.dto.PlanOption;
import com.safar.insurance.entity.enums.CoverageType;

import java.util.List;

/**
 * Canonical add-on / rider catalog for the SANDBOX underwriter — single source of truth
 * shared by SandboxInsuranceProvider (to advertise add-ons on each plan) and the marketplace
 * buy flow (to price the customer's selected add-ons server-side). For a live AGGREGATOR the
 * add-on prices come from the partner API instead.
 */
public final class InsuranceAddOnCatalog {

    private InsuranceAddOnCatalog() {}

    public static List<PlanOption.AddOn> addOns(CoverageType c) {
        return switch (c) {
            case HEALTH -> List.of(
                    new PlanOption.AddOn("ROOM_RENT_WAIVER", "Room-rent waiver", 80000),
                    new PlanOption.AddOn("OPD", "OPD cover", 150000),
                    new PlanOption.AddOn("MATERNITY", "Maternity cover", 250000));
            case LIFE_TERM -> List.of(
                    new PlanOption.AddOn("ADB", "Accidental death benefit", 90000),
                    new PlanOption.AddOn("CI", "Critical illness rider", 220000),
                    new PlanOption.AddOn("WOP", "Waiver of premium", 60000));
            case MOTOR -> List.of(
                    new PlanOption.AddOn("ZERO_DEP", "Zero depreciation", 120000),
                    new PlanOption.AddOn("ENGINE", "Engine protect", 90000),
                    new PlanOption.AddOn("RSA", "24x7 roadside assistance", 40000));
            case INTERNATIONAL_TRAVEL, DOMESTIC_TRAVEL, STUDENT_TRAVEL -> List.of(
                    new PlanOption.AddOn("ADVENTURE", "Adventure sports cover", 50000),
                    new PlanOption.AddOn("GADGET", "Gadget & laptop cover", 30000),
                    new PlanOption.AddOn("CANCEL_PLUS", "Cancel-for-any-reason", 70000));
            default -> List.of();
        };
    }

    /** Sum the premium (paise) of the selected add-on codes for a coverage type. */
    public static long priceOf(CoverageType c, List<String> selectedCodes) {
        if (selectedCodes == null || selectedCodes.isEmpty()) return 0;
        long total = 0;
        for (PlanOption.AddOn a : addOns(c)) {
            if (selectedCodes.contains(a.code())) total += a.premiumPaise();
        }
        return total;
    }
}
