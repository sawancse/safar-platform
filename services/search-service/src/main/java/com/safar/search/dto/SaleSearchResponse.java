package com.safar.search.dto;

import com.safar.search.document.SalePropertyDocument;

import java.util.List;
import java.util.Map;

public record SaleSearchResponse(
        List<SalePropertyDocument> content,
        long totalHits,
        int page,
        int size,
        Map<String, Map<String, Long>> aggregations
) {}
