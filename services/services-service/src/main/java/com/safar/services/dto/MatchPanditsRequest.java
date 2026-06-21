package com.safar.services.dto;

/**
 * Criteria for matching a customer to verified pandit listings. All fields are
 * optional — every supplied criterion that a pandit satisfies adds to the score.
 */
public record MatchPanditsRequest(
        String occasion,        // HOUSEWARMING, MARRIAGE, BABY, ... (frontend occasion key)
        String pujaType,        // GRIHA_PRAVESH, SATYANARAYAN, WEDDING, MUNDAN, ... (matches pujaTypesOffered)
        String language,        // preferred performance language e.g. "Telugu"
        String tradition,       // SMARTA, VAISHNAV, IYER, IYENGAR, ...
        String gotra,           // customer's gotra (matched against pandit_gotra)
        String city,            // home city to prefer local pandits
        Boolean onlineOk        // customer open to a video-call puja
) {}
