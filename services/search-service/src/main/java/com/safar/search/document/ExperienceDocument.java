package com.safar.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "experiences", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String locationName;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Double)
    private Double durationHours;

    @Field(type = FieldType.Integer)
    private Integer maxGuests;

    @Field(type = FieldType.Long)
    private Long pricePaise;

    @Field(type = FieldType.Keyword)
    private String languagesSpoken;

    @Field(type = FieldType.Keyword)
    private String mediaUrls;

    @Field(type = FieldType.Keyword)
    private String cancellationPolicy;

    @Field(type = FieldType.Boolean)
    private Boolean isPrivate;

    @Field(type = FieldType.Integer)
    private Integer minAge;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword)
    private String hostId;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX||uuuu-MM-dd'T'HH:mm:ssXXX||epoch_millis")
    private String indexedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private Double lat;
        private Double lon;
    }
}
