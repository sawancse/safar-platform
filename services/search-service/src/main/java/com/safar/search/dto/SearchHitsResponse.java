package com.safar.search.dto;

import com.safar.search.document.ListingDocument;

import java.util.List;

public record SearchHitsResponse(
        List<ListingDocument> listings,
        long totalHits,
        int page,
        int size,
        FilterAggregations aggregations
) {
    // Backwards-compatible constructor
    public SearchHitsResponse(List<ListingDocument> listings, long totalHits, int page, int size) {
        this(listings, totalHits, page, size, null);
    }
}
