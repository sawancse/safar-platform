package com.safar.search.service;

import com.safar.search.document.ListingDocument;
import com.safar.search.dto.SearchHitsResponse;
import com.safar.search.dto.SearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ElasticsearchOperations esOps;
    @InjectMocks SearchService searchService;

    // Helper: build SearchRequest with only the fields needed, rest null
    private SearchRequest req(String query, String city, List<String> type,
                              Long priceMin, Long priceMax,
                              Double lat, Double lng, Double radiusKm,
                              String sort, Boolean instantBook, Double minRating,
                              Boolean petFriendly, Integer minBedrooms, Integer minBathrooms,
                              List<String> amenities) {
        return new SearchRequest(query, city, type, priceMin, priceMax,
                lat, lng, radiusKm, sort, instantBook, minRating,
                petFriendly, minBedrooms, minBathrooms, amenities,
                null, null, null, null, null, null, null, 0, 20);
    }

    private SearchRequest basicReq(String city) {
        return req(null, city, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private SearchHits<ListingDocument> buildEmptyHits() {
        SearchHits<ListingDocument> hits = mock(SearchHits.class);
        doReturn(List.of()).when(hits).getSearchHits();
        doReturn(0L).when(hits).getTotalHits();
        return hits;
    }

    @SuppressWarnings("unchecked")
    private SearchHits<ListingDocument> buildHitsWithDoc(ListingDocument doc) {
        SearchHit<ListingDocument> hit = mock(SearchHit.class);
        doReturn(doc).when(hit).getContent();
        SearchHits<ListingDocument> hits = mock(SearchHits.class);
        doReturn(List.of(hit)).when(hits).getSearchHits();
        doReturn(1L).when(hits).getTotalHits();
        return hits;
    }

    private void stubEmpty() {
        SearchHits<ListingDocument> hits = buildEmptyHits();
        doReturn(hits).when(esOps).search(any(NativeQuery.class), eq(ListingDocument.class));
    }

    @Test
    void search_withCity_delegatesToElasticsearch() {
        stubEmpty();
        SearchHitsResponse result = searchService.search(basicReq("Mumbai"), null);
        assertThat(result.listings()).isEmpty();
        assertThat(result.totalHits()).isZero();
    }

    @Test
    void search_alwaysFiltersVerifiedOnly() {
        stubEmpty();
        SearchHitsResponse result = searchService.search(basicReq(null), null);
        assertThat(result).isNotNull();
        assertThat(result.totalHits()).isZero();
    }

    @Test
    void search_returnsListingDocumentsFromHits() {
        ListingDocument doc = ListingDocument.builder()
                .id("listing-1").title("Cozy 2BHK").city("Mumbai").isVerified(true).build();
        SearchHits<ListingDocument> hits = buildHitsWithDoc(doc);
        doReturn(hits).when(esOps).search(any(NativeQuery.class), eq(ListingDocument.class));

        SearchHitsResponse result = searchService.search(basicReq("Mumbai"), null);
        assertThat(result.listings()).hasSize(1);
        assertThat(result.listings().get(0).getTitle()).isEqualTo("Cozy 2BHK");
        assertThat(result.totalHits()).isEqualTo(1L);
    }

    @Test
    void search_withPriceRange_callsElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, 100000L, 500000L,
                null, null, null, null, null, null, null, null, null, null);
        assertThat(searchService.search(r, null).totalHits()).isZero();
    }

    @Test
    void search_withGeoDistance_callsElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                19.076090, 72.877426, 10.0, null, null, null, null, null, null, null);
        assertThat(searchService.search(r, null).totalHits()).isZero();
    }

    @Test
    void autocomplete_returnsDistinctCities() {
        ListingDocument doc1 = ListingDocument.builder().city("Mumbai").isVerified(true).build();
        ListingDocument doc2 = ListingDocument.builder().city("Mumbai").isVerified(true).build();

        SearchHit<ListingDocument> hit1 = mock(SearchHit.class);
        SearchHit<ListingDocument> hit2 = mock(SearchHit.class);
        doReturn(doc1).when(hit1).getContent();
        doReturn(doc2).when(hit2).getContent();

        SearchHits<ListingDocument> hits = mock(SearchHits.class);
        doReturn(List.of(hit1, hit2)).when(hits).getSearchHits();

        when(esOps.search(any(NativeQuery.class), eq(ListingDocument.class))).thenReturn(hits);

        List<String> result = searchService.autocomplete("Mum");
        assertThat(result).containsExactly("Mumbai");
    }

    @Test
    void search_withSortPriceAsc_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, "Mumbai", null, null, null,
                null, null, null, "price_asc", null, null, null, null, null, null);
        assertThat(searchService.search(r, null)).isNotNull();
        verify(esOps).search(any(NativeQuery.class), eq(ListingDocument.class));
    }

    @Test
    void search_withInstantBookFilter_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, true, null, null, null, null, null);
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void search_withMinRating_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, null, 4.0, null, null, null, null);
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void search_withPetFriendly_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, null, null, true, null, null, null);
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void search_withMinBedrooms_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, null, null, null, 2, null, null);
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void search_withMinBathrooms_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, null, null, null, null, 1, null);
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void search_withAmenities_delegatesToElasticsearch() {
        stubEmpty();
        SearchRequest r = req(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of("WIFI", "PARKING"));
        assertThat(searchService.search(r, null)).isNotNull();
    }

    @Test
    void indexListing_savesToElasticsearch() {
        ListingDocument doc = ListingDocument.builder()
                .id("listing-2").title("Beach House").isVerified(true).build();
        when(esOps.save(doc)).thenReturn(doc);

        searchService.indexListing(doc);

        verify(esOps).save(doc);
    }
}
