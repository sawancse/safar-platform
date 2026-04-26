package com.safar.chef.dto;

import com.safar.chef.entity.enums.KycDocumentType;

import java.time.LocalDate;

public record UploadKycDocumentRequest(
        KycDocumentType documentType,
        String documentUrl,         // S3 URL, uploaded via media-service first
        String documentNumber,      // FSSAI #, Aadhaar #, etc.
        LocalDate expiresAt
) {}
