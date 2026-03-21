package com.safar.search.service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses natural-language search queries into structured filters.
 * Examples:
 *   "Home in Hyderabad"         → type=HOME, city=Hyderabad
 *   "PG near Gachibowli"       → type=PG, city=Gachibowli
 *   "Villa, Goa"               → type=VILLA, city=Goa
 *   "2BHK apartment Mumbai"    → type=APARTMENT, city=Mumbai, query=2BHK
 *   "budget hotel Jaipur"      → type=BUDGET_HOTEL, city=Jaipur
 *   "Hyderabad, Telangana"     → city=Hyderabad
 */
public class SmartQueryParser {

    // Property type keywords → listing type enum
    private static final Map<String, String> TYPE_KEYWORDS = new LinkedHashMap<>();
    static {
        TYPE_KEYWORDS.put("budget hotel", "BUDGET_HOTEL");
        TYPE_KEYWORDS.put("farm stay", "FARMSTAY");
        TYPE_KEYWORDS.put("farmstay", "FARMSTAY");
        TYPE_KEYWORDS.put("co-living", "COLIVING");
        TYPE_KEYWORDS.put("coliving", "COLIVING");
        TYPE_KEYWORDS.put("co living", "COLIVING");
        TYPE_KEYWORDS.put("unique stay", "UNIQUE");
        TYPE_KEYWORDS.put("unique", "UNIQUE");
        TYPE_KEYWORDS.put("home", "HOME");
        TYPE_KEYWORDS.put("homes", "HOME");
        TYPE_KEYWORDS.put("house", "HOME");
        TYPE_KEYWORDS.put("room", "ROOM");
        TYPE_KEYWORDS.put("rooms", "ROOM");
        TYPE_KEYWORDS.put("villa", "VILLA");
        TYPE_KEYWORDS.put("villas", "VILLA");
        TYPE_KEYWORDS.put("resort", "RESORT");
        TYPE_KEYWORDS.put("resorts", "RESORT");
        TYPE_KEYWORDS.put("homestay", "HOMESTAY");
        TYPE_KEYWORDS.put("homestays", "HOMESTAY");
        TYPE_KEYWORDS.put("hostel", "HOSTEL");
        TYPE_KEYWORDS.put("hostels", "HOSTEL");
        TYPE_KEYWORDS.put("hotel", "HOTEL");
        TYPE_KEYWORDS.put("hotels", "HOTEL");
        TYPE_KEYWORDS.put("pg", "PG");
        TYPE_KEYWORDS.put("paying guest", "PG");
        TYPE_KEYWORDS.put("apartment", "APARTMENT");
        TYPE_KEYWORDS.put("apartments", "APARTMENT");
        TYPE_KEYWORDS.put("flat", "APARTMENT");
        TYPE_KEYWORDS.put("flats", "APARTMENT");
        TYPE_KEYWORDS.put("cottage", "COTTAGE");
        TYPE_KEYWORDS.put("lodge", "LODGE");
        TYPE_KEYWORDS.put("commercial", "COMMERCIAL");
        TYPE_KEYWORDS.put("office", "COMMERCIAL");
        TYPE_KEYWORDS.put("guesthouse", "GUESTHOUSE");
        TYPE_KEYWORDS.put("guest house", "GUESTHOUSE");
    }

    // Known Indian cities for extraction
    private static final Set<String> KNOWN_CITIES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        KNOWN_CITIES.addAll(List.of(
            "Mumbai", "Delhi", "Bangalore", "Bengaluru", "Hyderabad", "Chennai", "Kolkata",
            "Pune", "Ahmedabad", "Jaipur", "Goa", "Kochi", "Lucknow", "Chandigarh",
            "Indore", "Bhopal", "Nagpur", "Surat", "Vadodara", "Coimbatore",
            "Manali", "Shimla", "Rishikesh", "Udaipur", "Jodhpur", "Jaisalmer",
            "Gokarna", "Pondicherry", "Ooty", "Munnar", "Darjeeling", "Gangtok",
            "Mysore", "Mysuru", "Hampi", "Varanasi", "Agra", "Amritsar",
            "Dehradun", "Mussoorie", "Nainital", "Lonavala", "Mahabaleshwar",
            "Alibaug", "Kodaikanal", "Wayanad", "Alleppey", "Coorg",
            "Leh", "Srinagar", "Guwahati", "Shillong", "Visakhapatnam",
            "Thiruvananthapuram", "Noida", "Gurgaon", "Gurugram", "Faridabad"
        ));
    }

    // Known Indian states
    private static final Set<String> KNOWN_STATES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        KNOWN_STATES.addAll(List.of(
            "Telangana", "Maharashtra", "Karnataka", "Tamil Nadu", "Kerala",
            "Rajasthan", "Goa", "Himachal Pradesh", "Uttarakhand", "West Bengal",
            "Gujarat", "Madhya Pradesh", "Uttar Pradesh", "Punjab", "Haryana",
            "Andhra Pradesh", "Odisha", "Jharkhand", "Bihar", "Assam",
            "Jammu and Kashmir", "Sikkim", "Meghalaya"
        ));
    }

    // Noise words to strip
    private static final Set<String> STOP_WORDS = Set.of(
        "in", "at", "near", "around", "close", "to", "the", "a", "an",
        "for", "with", "and", "or", "best", "top", "cheap", "affordable"
    );

    public record ParsedQuery(
        String cleanedQuery,   // remaining text after extraction (null if fully parsed)
        String extractedCity,  // city found in query (null if none)
        String extractedType,  // listing type found (null if none)
        String extractedLocality, // locality/area found (not a city, e.g. "Gachibowli")
        boolean fullyParsed    // true if nothing left to text-search
    ) {}

    public static ParsedQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new ParsedQuery(null, null, null, null, true);
        }

        String input = rawQuery.trim();

        // Split by common delimiters: comma, "in", "near", "at"
        String normalized = input
            .replaceAll("[,;|]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        String extractedType = null;
        String extractedCity = null;

        // 1. Extract property type (longest match first — "budget hotel" before "hotel")
        String lowerNorm = normalized.toLowerCase();
        for (var entry : TYPE_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            if (lowerNorm.contains(keyword)) {
                extractedType = entry.getValue();
                // Remove the keyword from the normalized string
                int idx = lowerNorm.indexOf(keyword);
                normalized = (normalized.substring(0, idx) + normalized.substring(idx + keyword.length())).trim();
                lowerNorm = normalized.toLowerCase();
                break;
            }
        }

        // 2. Extract city (check each remaining word/phrase against known cities)
        String[] parts = normalized.split("\\s+");
        List<String> remaining = new ArrayList<>();
        for (String part : parts) {
            String clean = part.replaceAll("[^a-zA-Z]", "");
            if (clean.isEmpty()) continue;
            if (KNOWN_CITIES.contains(clean)) {
                extractedCity = clean;
            } else if (KNOWN_STATES.contains(clean)) {
                // State name — skip it (city is more useful for filtering)
            } else if (STOP_WORDS.contains(clean.toLowerCase())) {
                // Skip noise
            } else {
                remaining.add(clean);
            }
        }

        // Also check two-word city names
        if (extractedCity == null) {
            for (int i = 0; i < parts.length - 1; i++) {
                String twoWord = parts[i].replaceAll("[^a-zA-Z]", "") + " " + parts[i + 1].replaceAll("[^a-zA-Z]", "");
                if (KNOWN_CITIES.contains(twoWord)) {
                    extractedCity = twoWord;
                    remaining.remove(parts[i].replaceAll("[^a-zA-Z]", ""));
                    remaining.remove(parts[i + 1].replaceAll("[^a-zA-Z]", ""));
                }
            }
        }

        // If we found a city and there's remaining text, treat it as locality/area
        String extractedLocality = null;
        if (extractedCity != null && !remaining.isEmpty()) {
            extractedLocality = String.join(" ", remaining);
            remaining.clear(); // locality absorbs remaining words
        }

        String cleanedQuery = remaining.isEmpty() ? null : String.join(" ", remaining);
        boolean fullyParsed = (cleanedQuery == null || cleanedQuery.isBlank()) && extractedLocality == null;

        return new ParsedQuery(
            fullyParsed ? null : cleanedQuery,
            extractedCity,
            extractedType,
            extractedLocality,
            fullyParsed
        );
    }
}
