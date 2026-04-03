package com.safar.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Document(indexName = "sale_properties", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalePropertyDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String salePropertyType;

    @Field(type = FieldType.Keyword)
    private String transactionType;

    @Field(type = FieldType.Keyword)
    private String sellerType;

    // Location
    @Field(type = FieldType.Text, analyzer = "standard")
    private String locality;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String address;

    @Field(type = FieldType.Keyword)
    private String pincode;

    @GeoPointField
    private GeoPoint location;

    // Pricing
    @Field(type = FieldType.Long)
    private Long askingPricePaise;

    @Field(type = FieldType.Long)
    private Long pricePerSqftPaise;

    @Field(type = FieldType.Boolean)
    private Boolean priceNegotiable;

    @Field(type = FieldType.Long)
    private Long maintenancePaise;

    // Area
    @Field(type = FieldType.Integer)
    private Integer carpetAreaSqft;

    @Field(type = FieldType.Integer)
    private Integer builtUpAreaSqft;

    @Field(type = FieldType.Integer)
    private Integer superBuiltUpAreaSqft;

    @Field(type = FieldType.Integer)
    private Integer plotAreaSqft;

    // Configuration
    @Field(type = FieldType.Integer)
    private Integer bedrooms;

    @Field(type = FieldType.Integer)
    private Integer bathrooms;

    @Field(type = FieldType.Integer)
    private Integer balconies;

    @Field(type = FieldType.Integer)
    private Integer floorNumber;

    @Field(type = FieldType.Integer)
    private Integer totalFloors;

    @Field(type = FieldType.Keyword)
    private String facing;

    @Field(type = FieldType.Integer)
    private Integer propertyAgeYears;

    @Field(type = FieldType.Keyword)
    private String furnishing;

    @Field(type = FieldType.Integer)
    private Integer parkingCovered;

    @Field(type = FieldType.Integer)
    private Integer parkingOpen;

    // Construction & Legal
    @Field(type = FieldType.Keyword)
    private String possessionStatus;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate possessionDate;

    @Field(type = FieldType.Keyword)
    private String builderName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String projectName;

    @Field(type = FieldType.Keyword)
    private String reraId;

    @Field(type = FieldType.Boolean)
    private Boolean reraVerified;

    // Features
    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    @Field(type = FieldType.Keyword)
    private String waterSupply;

    @Field(type = FieldType.Keyword)
    private String powerBackup;

    @Field(type = FieldType.Boolean)
    private Boolean gatedCommunity;

    @Field(type = FieldType.Boolean)
    private Boolean cornerProperty;

    @Field(type = FieldType.Boolean)
    private Boolean vastuCompliant;

    @Field(type = FieldType.Boolean)
    private Boolean petAllowed;

    // Media
    @Field(type = FieldType.Keyword)
    private String primaryPhotoUrl;

    // Status
    @Field(type = FieldType.Boolean)
    private Boolean verified;

    @Field(type = FieldType.Boolean)
    private Boolean featured;

    @Field(type = FieldType.Integer)
    private Integer viewsCount;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX||uuuu-MM-dd'T'HH:mm:ssXXX||epoch_millis")
    private String indexedAt;

    // Inner class for geo point
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private Double lat;
        private Double lon;
    }
}
