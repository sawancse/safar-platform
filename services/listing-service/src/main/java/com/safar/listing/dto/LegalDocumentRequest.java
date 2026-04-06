package com.safar.listing.dto;

public record LegalDocumentRequest(
        String documentType,
        String fileUrl
) {}
