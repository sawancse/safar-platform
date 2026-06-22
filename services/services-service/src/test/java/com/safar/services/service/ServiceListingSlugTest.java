package com.safar.services.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for the vendor-slug normalizer (the slugify guard). */
class ServiceListingSlugTest {

    @Test
    void normalizesSpacesAndCase() {
        assertEquals("durga-pandit", ServiceListingService.slugify("Durga Pandit"));
        assertEquals("pandit-durga-pooja", ServiceListingService.slugify("Pandit Durga Pooja"));
        assertEquals("all-type-of-decoration", ServiceListingService.slugify("all type of decoration"));
    }

    @Test
    void keepsAlphanumericAndCollapsesSeparators() {
        assertEquals("ram-decor-company123", ServiceListingService.slugify("Ram Decor Company123"));
        assertEquals("a-b-c", ServiceListingService.slugify("a---b   c"));
        assertEquals("hello-world", ServiceListingService.slugify("  Hello, World!!  "));
    }

    @Test
    void stripsAccents() {
        assertEquals("cafe-musica", ServiceListingService.slugify("Café Música"));
    }

    @Test
    void trimsLeadingAndTrailingSeparators() {
        assertEquals("pandit", ServiceListingService.slugify("--Pandit--"));
        assertEquals("pandit", ServiceListingService.slugify("!!Pandit!!"));
    }

    @Test
    void emptyOrSymbolOnlyBecomesEmpty() {
        assertEquals("", ServiceListingService.slugify(null));
        assertEquals("", ServiceListingService.slugify("   "));
        assertEquals("", ServiceListingService.slugify("!!!"));
    }
}
