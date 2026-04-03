package com.safar.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "listings", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDocument {

    @Id
    private String id;

    private String title;
    private String description;
    @Field(type = FieldType.Keyword)
    private String type;
    @Field(type = FieldType.Keyword)
    private String commercialCategory;
    private String city;
    private String state;
    private String pincode;
    private String address; // addressLine1 + addressLine2 + locality for search

    @GeoPointField
    private GeoPoint location;

    private Long basePricePaise;
    @Field(type = FieldType.Keyword)
    private String pricingUnit;
    private Integer maxGuests;
    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    private Double avgRating;
    private Integer reviewCount;

    private Boolean isVerified;
    private Boolean instantBook;
    private Boolean aiPricingEnabled;
    private Boolean isRemoteCertified;
    private Boolean petFriendly;

    private Integer bedrooms;
    private Integer bathrooms;

    private Integer starRating;
    @Field(type = FieldType.Keyword)
    private String cancellationPolicy;
    @Field(type = FieldType.Keyword)
    private String mealPlan;
    @Field(type = FieldType.Keyword)
    private List<String> bedTypes;
    @Field(type = FieldType.Keyword)
    private List<String> accessibilityFeatures;
    private Boolean freeCancellation;
    private Boolean noPrepayment;

    private Boolean aashrayReady;

    private Boolean medicalStay;
    private List<String> hospitalNames;
    @Field(type = FieldType.Keyword)
    private List<String> medicalSpecialties;
    @Field(type = FieldType.Keyword)
    private List<String> procedureNames;

    // PG/Co-living fields
    @Field(type = FieldType.Keyword)
    private String occupancyType;
    @Field(type = FieldType.Keyword)
    private String foodType;
    private Long securityDepositPaise;

    // Hotel fields
    private Boolean frontDesk24h;

    private Long cleaningFeePaise;

    private Integer visibilityBoostPercent;
    private Boolean preferredPartner;

    private String primaryPhotoUrl;
    private String primaryVideoUrl;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS||epoch_millis")
    private LocalDateTime indexedAt;
}
