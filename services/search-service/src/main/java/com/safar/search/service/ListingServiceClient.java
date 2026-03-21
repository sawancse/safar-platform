package com.safar.search.service;

import com.safar.search.document.ListingDocument;

import java.util.UUID;

public interface ListingServiceClient {
    ListingDocument getListingDocument(UUID listingId);
}
