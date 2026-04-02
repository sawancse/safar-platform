package com.safar.search.service;

import java.util.*;

/**
 * Parses natural-language search queries into structured filters.
 * Examples:
 *   "Home in Hyderabad"         → type=HOME, city=Hyderabad
 *   "PG near Gachibowli"       → type=PG, locality=Gachibowli
 *   "Villa, Goa"               → type=VILLA, city=Goa
 *   "2BHK apartment Mumbai"    → type=APARTMENT, city=Mumbai, query=2BHK
 *   "budget hotel Jaipur"      → type=BUDGET_HOTEL, city=Jaipur
 *   "flat in madhapur"         → type=APARTMENT, locality=Madhapur
 *   "resort near me"           → type=RESORT, nearMe=true
 *   "Madhapur"                 → locality=Madhapur
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
            "Thiruvananthapuram", "Noida", "Gurgaon", "Gurugram", "Faridabad",
            "Cherrapunji", "Kasol", "Mcleodganj", "Dharamshala", "Pushkar",
            "Ranthambore", "Khajuraho", "Kovalam", "Thekkady", "Varkala",
            "Lakshadweep", "Andaman", "Daman", "Diu", "Ganpatipule",
            "Bhubaneswar", "Cuttack", "Ranchi", "Patna", "Trivandrum",
            "Navi Mumbai", "Thane", "Secunderabad"
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

    // Well-known Indian localities/areas (not cities)
    private static final Set<String> KNOWN_LOCALITIES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        KNOWN_LOCALITIES.addAll(List.of(
            // Hyderabad
            "Madhapur", "Gachibowli", "Hitech City", "Kondapur", "Kukatpally",
            "Banjara Hills", "Jubilee Hills", "Ameerpet", "Begumpet", "Miyapur",
            "Manikonda", "Tolichowki", "Uppal", "Dilsukhnagar", "LB Nagar",
            "ECIL", "Secunderabad", "Kompally", "Shamshabad", "Nanakramguda",
            "Financial District", "Raidurg",
            // Bangalore
            "Koramangala", "Indiranagar", "HSR Layout", "Whitefield", "Electronic City",
            "BTM Layout", "Jayanagar", "JP Nagar", "Marathahalli", "Sarjapur",
            "Hebbal", "Yelahanka", "Bannerghatta", "Banashankari", "Rajajinagar",
            "MG Road", "Brigade Road", "Bellandur", "Kadugodi", "Domlur",
            // Mumbai
            "Andheri", "Bandra", "Juhu", "Powai", "Malad", "Goregaon",
            "Borivali", "Dadar", "Lower Parel", "Worli", "Colaba",
            "Marine Drive", "Churchgate", "Fort", "BKC", "Kurla",
            "Thane", "Navi Mumbai", "Vashi", "Belapur", "Panvel", "Kharghar",
            // Delhi NCR
            "Connaught Place", "Karol Bagh", "Saket", "Hauz Khas", "Dwarka",
            "Rohini", "Lajpat Nagar", "South Extension", "Defence Colony",
            "Greater Kailash", "Vasant Kunj", "Janakpuri", "Pitampura",
            "Sarita Vihar", "Jasola", "Nehru Place", "Rajouri Garden",
            // Gurgaon
            "Cyber City", "Golf Course Road", "Sohna Road", "MG Road Gurgaon",
            "DLF Phase", "Sector 29", "Sector 14",
            // Pune
            "Koregaon Park", "Hinjewadi", "Viman Nagar", "Kharadi", "Wakad",
            "Baner", "Aundh", "Shivajinagar", "Deccan", "Hadapsar", "Magarpatta",
            "Kothrud", "Pimpri", "Chinchwad",
            // Chennai
            "Anna Nagar", "T Nagar", "Adyar", "Velachery", "Nungambakkam",
            "Mylapore", "OMR", "ECR", "Porur", "Tambaram", "Sholinganallur",
            "Guindy", "Greams Road",
            // Kolkata
            "Park Street", "Salt Lake", "New Town", "Rajarhat", "Ballygunge",
            "Alipore", "Howrah", "Dum Dum", "Gariahat",
            // Jaipur
            "Malviya Nagar", "Vaishali Nagar", "Mansarovar", "C Scheme",
            "MI Road", "Tonk Road", "Sanganer",
            // Ahmedabad
            "SG Highway", "Prahlad Nagar", "Satellite", "Navrangpura", "CG Road",
            "Bodakdev", "Vastrapur", "Thaltej",
            // Goa
            "Calangute", "Baga", "Anjuna", "Vagator", "Palolem",
            "Candolim", "Arambol", "Panaji", "Margao", "Dona Paula",
            "Morjim", "Ashwem", "South Goa", "North Goa"
        ));
    }

    // Noise words to strip
    private static final Set<String> STOP_WORDS = Set.of(
        "in", "at", "near", "around", "close", "to", "the", "a", "an",
        "for", "with", "and", "or", "best", "top", "cheap", "affordable"
    );

    public record ParsedQuery(
        String cleanedQuery,      // remaining text after extraction (null if fully parsed)
        String extractedCity,     // city found in query (null if none)
        String extractedType,     // listing type found (null if none)
        String extractedLocality, // locality/area found (e.g. "Madhapur", "Gachibowli")
        boolean nearMe,           // true if user typed "near me"
        boolean fullyParsed       // true if nothing left to text-search
    ) {
        // Backwards-compatible constructor without nearMe
        public ParsedQuery(String cleanedQuery, String extractedCity, String extractedType,
                           String extractedLocality, boolean fullyParsed) {
            this(cleanedQuery, extractedCity, extractedType, extractedLocality, false, fullyParsed);
        }
    }

    public static ParsedQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new ParsedQuery(null, null, null, null, false, true);
        }

        String input = rawQuery.trim();

        // Detect "near me" pattern
        boolean nearMe = input.toLowerCase().matches(".*\\bnear\\s+me\\b.*");

        // Split by common delimiters: comma, "in", "near", "at"
        String normalized = input
            .replaceAll("[,;|]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        // Remove "near me" from text before further parsing
        if (nearMe) {
            normalized = normalized.replaceAll("(?i)\\bnear\\s+me\\b", "").trim();
        }

        String extractedType = null;
        String extractedCity = null;

        // 1. Extract property type (longest match first — "budget hotel" before "hotel")
        String lowerNorm = normalized.toLowerCase();
        for (var entry : TYPE_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            if (lowerNorm.contains(keyword)) {
                extractedType = entry.getValue();
                int idx = lowerNorm.indexOf(keyword);
                normalized = (normalized.substring(0, idx) + normalized.substring(idx + keyword.length())).trim();
                lowerNorm = normalized.toLowerCase();
                break;
            }
        }

        // 2. Extract city and locality from remaining words
        String[] parts = normalized.split("\\s+");
        List<String> remaining = new ArrayList<>();
        String extractedLocality = null;

        // First pass: check for two-word matches (cities and localities)
        Set<Integer> consumed = new HashSet<>();
        for (int i = 0; i < parts.length - 1; i++) {
            String twoWord = parts[i].replaceAll("[^a-zA-Z]", "") + " " + parts[i + 1].replaceAll("[^a-zA-Z]", "");
            if (KNOWN_CITIES.contains(twoWord)) {
                extractedCity = twoWord;
                consumed.add(i);
                consumed.add(i + 1);
                break;
            } else if (KNOWN_LOCALITIES.contains(twoWord)) {
                extractedLocality = twoWord;
                consumed.add(i);
                consumed.add(i + 1);
                break;
            }
        }

        // Second pass: single words
        for (int i = 0; i < parts.length; i++) {
            if (consumed.contains(i)) continue;
            String clean = parts[i].replaceAll("[^a-zA-Z]", "");
            if (clean.isEmpty()) continue;

            if (extractedCity == null && KNOWN_CITIES.contains(clean)) {
                extractedCity = clean;
            } else if (extractedLocality == null && KNOWN_LOCALITIES.contains(clean)) {
                extractedLocality = clean;
            } else if (KNOWN_STATES.contains(clean)) {
                // Skip state names
            } else if (STOP_WORDS.contains(clean.toLowerCase())) {
                // Skip noise
            } else if ("me".equalsIgnoreCase(clean)) {
                // Skip "me" (from "near me" partial removal)
            } else {
                remaining.add(clean);
            }
        }

        // If we found a city but no locality, and there's remaining text that looks like a place name,
        // treat it as locality
        if (extractedCity != null && extractedLocality == null && !remaining.isEmpty()) {
            extractedLocality = String.join(" ", remaining);
            remaining.clear();
        }

        // If no city AND no locality found, but remaining has a single word that could be a locality,
        // treat it as locality (search address field) — covers "madhapur" typed alone
        if (extractedCity == null && extractedLocality == null && !remaining.isEmpty()) {
            // If we have a type and remaining words, those words are likely a location
            if (extractedType != null) {
                extractedLocality = String.join(" ", remaining);
                remaining.clear();
            }
            // If no type either, still treat single remaining word as potential locality
            // (will be searched in address + city fields via text search)
        }

        String cleanedQuery = remaining.isEmpty() ? null : String.join(" ", remaining);
        boolean fullyParsed = (cleanedQuery == null || cleanedQuery.isBlank());

        return new ParsedQuery(
            fullyParsed ? null : cleanedQuery,
            extractedCity,
            extractedType,
            extractedLocality,
            nearMe,
            fullyParsed
        );
    }
}
