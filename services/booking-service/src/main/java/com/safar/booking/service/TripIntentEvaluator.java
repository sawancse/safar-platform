package com.safar.booking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.entity.Trip;
import com.safar.booking.entity.TripIntentRule;
import com.safar.booking.entity.enums.LegType;
import com.safar.booking.entity.enums.TripIntent;
import com.safar.booking.repository.TripIntentRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Cross-vertical suggestion engine — Day-1 rule-based.
 *
 * Walks the {@code trip_intent_rules} table in priority order, evaluates
 * each rule's trigger against the Trip context, picks the lowest-priority
 * matches, UNIONS their suggested verticals, and filters out any vertical
 * that isn't in the {@code LIVE_VERTICALS} config (so Phase-2 verticals
 * like PANDIT can have rules pre-seeded but won't surface until enabled).
 *
 * Phase-3 will replace this with an ML Trip-DNA model behind the same
 * input/output contract.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripIntentEvaluator {

    private final TripIntentRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    /**
     * Comma-separated list of LegType names that are LIVE Day-1.
     * Default = STAY,CAB,COOK,INSURANCE per Phase-1 Param-set #8.
     */
    @Value("${trip.live-verticals:STAY,CAB,COOK,INSURANCE}")
    private String liveVerticalsCsv;

    public Result evaluate(TripContext ctx) {
        String country = ctx.destinationCountry != null ? ctx.destinationCountry : "IN";
        List<TripIntentRule> rules = ruleRepository.findEnabledForCountryByPriority(country);

        // Find all matches at the lowest priority bucket that has any match.
        Integer winningPriority = null;
        List<TripIntentRule> winners = new ArrayList<>();
        for (TripIntentRule rule : rules) {
            if (winningPriority != null && rule.getPriority() > winningPriority) break; // stop at first miss past the winning bucket
            if (matches(rule, ctx)) {
                if (winningPriority == null) winningPriority = rule.getPriority();
                winners.add(rule);
            }
        }

        if (winners.isEmpty()) {
            log.debug("No rules matched for trip ctx {} → fallback UNCLASSIFIED", ctx);
            return new Result(TripIntent.UNCLASSIFIED, List.of(LegType.STAY), Map.of(), List.of());
        }

        // UNION suggested verticals across winners; first winner's intent is canonical
        TripIntent intent = winners.get(0).getInferredIntent();
        Set<String> verticals = new LinkedHashSet<>();
        Map<String, JsonNode> filters = new LinkedHashMap<>();
        for (TripIntentRule w : winners) {
            if (w.getSuggestedVerticals() != null) {
                Collections.addAll(verticals, w.getSuggestedVerticals());
            }
            if (w.getVerticalFilters() != null && !w.getVerticalFilters().isBlank()) {
                try {
                    JsonNode filterNode = objectMapper.readTree(w.getVerticalFilters());
                    filterNode.fields().forEachRemaining(e -> filters.put(e.getKey(), e.getValue()));
                } catch (Exception e) {
                    log.warn("Bad verticalFilters JSON on rule {}: {}", w.getRuleName(), e.getMessage());
                }
            }
        }

        // Filter to LIVE verticals only — Phase-2 (PANDIT/DECOR/SPA/EXPERIENCE) silently dropped
        Set<String> live = new HashSet<>(Arrays.asList(liveVerticalsCsv.split(",")));
        List<LegType> out = verticals.stream()
                .filter(live::contains)
                .map(this::safeLegType)
                .filter(Objects::nonNull)
                .toList();

        return new Result(intent, out, filters, winners.stream().map(TripIntentRule::getRuleName).toList());
    }

    /**
     * Convenience: build a TripContext from a persisted Trip + evaluate.
     * Used by TripController.suggestions().
     */
    public Result evaluateForTrip(Trip trip) {
        return evaluateForTrip(trip, Set.of());
    }

    public Result evaluateForTrip(Trip trip, Set<String> userFlags) {
        TripContext ctx = new TripContext();
        ctx.origin = trip.getOriginCode();             // IATA (e.g. 'BLR') from V47
        ctx.destination = trip.getDestinationCode();
        ctx.originCity = trip.getOriginCity();
        ctx.destinationCity = trip.getDestinationCity();
        ctx.originCountry = trip.getOriginCountry();
        ctx.destinationCountry = trip.getDestinationCountry();
        ctx.departureDate = trip.getStartDate();
        ctx.returnDate = trip.getEndDate();
        ctx.pax = trip.getPaxCount() != null ? trip.getPaxCount() : 1;
        ctx.userFlags = userFlags != null ? userFlags : Set.of();
        return evaluate(ctx);
    }

    // ── Matching logic per trigger type ─────────────────────────

    private boolean matches(TripIntentRule rule, TripContext ctx) {
        try {
            JsonNode trigger = rule.getTriggerValue() != null && !rule.getTriggerValue().isBlank()
                    ? objectMapper.readTree(rule.getTriggerValue())
                    : objectMapper.createObjectNode();

            return switch (rule.getTriggerType()) {
                case "FALLBACK" -> true;
                case "DESTINATION" -> matchesDestination(trigger, ctx);
                case "ROUTE" -> matchesRoute(trigger, ctx);
                case "DATE" -> matchesDate(trigger, ctx);
                case "GROUP" -> matchesGroup(trigger, ctx);
                case "HISTORY" -> matchesHistory(trigger, ctx);
                case "COMPOUND" -> matchesDate(trigger, ctx)
                                && matchesGroup(trigger, ctx)
                                && matchesHistory(trigger, ctx)
                                && matchesRoute(trigger, ctx)
                                && matchesDestination(trigger, ctx);
                default -> false;
            };
        } catch (Exception e) {
            log.warn("Rule {} threw during evaluation: {}", rule.getRuleName(), e.getMessage());
            return false;
        }
    }

    private static boolean matchesDestination(JsonNode trigger, TripContext ctx) {
        JsonNode cities = trigger.get("cities");
        if (cities == null || !cities.isArray() || ctx.destination == null) return cities == null; // absent ⇒ "any"
        for (JsonNode c : cities) {
            if (c.asText().equalsIgnoreCase(ctx.destination)) return true;
        }
        return false;
    }

    private static boolean matchesRoute(JsonNode trigger, TripContext ctx) {
        JsonNode routes = trigger.get("routes");
        if (routes == null || !routes.isArray() || ctx.origin == null || ctx.destination == null) return routes == null;
        for (JsonNode pair : routes) {
            if (pair.isArray() && pair.size() == 2) {
                String r0 = pair.get(0).asText();
                String r1 = pair.get(1).asText();
                if (r0.equalsIgnoreCase(ctx.origin) && r1.equalsIgnoreCase(ctx.destination)) return true;
            }
        }
        return false;
    }

    private static boolean matchesDate(JsonNode trigger, TripContext ctx) {
        if (ctx.departureDate == null) return !trigger.has("month") && !trigger.has("day_range");
        // Month match (single int or array)
        JsonNode monthNode = trigger.get("month");
        if (monthNode != null) {
            int dm = ctx.departureDate.getMonthValue();
            if (monthNode.isInt() && monthNode.asInt() != dm) return false;
            if (monthNode.isArray()) {
                boolean ok = false;
                for (JsonNode m : monthNode) if (m.asInt() == dm) { ok = true; break; }
                if (!ok) return false;
            }
        }
        // Day range
        JsonNode dayRange = trigger.get("day_range");
        if (dayRange != null && dayRange.isArray() && dayRange.size() == 2) {
            int day = ctx.departureDate.getDayOfMonth();
            int lo = dayRange.get(0).asInt();
            int hi = dayRange.get(1).asInt();
            if (day < lo || day > hi) return false;
        }
        return true;
    }

    private static boolean matchesGroup(JsonNode trigger, TripContext ctx) {
        JsonNode paxNode = trigger.get("pax");
        if (paxNode != null && paxNode.asInt() != ctx.pax) return false;
        JsonNode minPax = trigger.get("min_pax");
        if (minPax != null && ctx.pax < minPax.asInt()) return false;
        JsonNode maxPax = trigger.get("max_pax");
        if (maxPax != null && ctx.pax > maxPax.asInt()) return false;
        return true;
    }

    private static boolean matchesHistory(JsonNode trigger, TripContext ctx) {
        JsonNode flag = trigger.get("user_flag");
        if (flag != null && (ctx.userFlags == null || !ctx.userFlags.contains(flag.asText()))) return false;
        return true;
    }

    private LegType safeLegType(String name) {
        try { return LegType.valueOf(name); } catch (Exception e) {
            log.warn("Unknown LegType in rule: {}", name);
            return null;
        }
    }

    // ── Public types ────────────────────────────────────────────

    public static class TripContext {
        public String origin;             // IATA code
        public String destination;        // IATA code
        public String originCity;         // human-readable
        public String destinationCity;    // human-readable
        public String originCountry;
        public String destinationCountry;
        public LocalDate departureDate;
        public LocalDate returnDate;
        public int pax;
        public Set<String> userFlags = Set.of();

        @Override
        public String toString() {
            return "TripContext{" + origin + "→" + destination + " on " + departureDate + ", pax=" + pax + "}";
        }
    }

    public record Result(
            TripIntent intent,
            List<LegType> suggestedVerticals,
            Map<String, JsonNode> verticalFilters,
            List<String> matchedRuleNames
    ) {}
}
