package com.safar.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Document(indexName = "builder_projects", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderProjectDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String projectName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String builderName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String locality;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String address;

    @GeoPointField
    private SalePropertyDocument.GeoPoint location;

    @Field(type = FieldType.Keyword)
    private String projectStatus; // UPCOMING, UNDER_CONSTRUCTION, READY_TO_MOVE

    @Field(type = FieldType.Integer)
    private Integer constructionProgressPercent;

    @Field(type = FieldType.Long)
    private Long minPricePaise;

    @Field(type = FieldType.Long)
    private Long maxPricePaise;

    @Field(type = FieldType.Integer)
    private Integer minBhk;

    @Field(type = FieldType.Integer)
    private Integer maxBhk;

    @Field(type = FieldType.Integer)
    private Integer minAreaSqft;

    @Field(type = FieldType.Integer)
    private Integer maxAreaSqft;

    @Field(type = FieldType.Integer)
    private Integer totalUnits;

    @Field(type = FieldType.Integer)
    private Integer availableUnits;

    @Field(type = FieldType.Integer)
    private Integer totalTowers;

    @Field(type = FieldType.Keyword)
    private String reraId;

    @Field(type = FieldType.Boolean)
    private Boolean reraVerified;

    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    @Field(type = FieldType.Keyword)
    private List<String> bankApprovals;

    @Field(type = FieldType.Keyword)
    private String primaryPhotoUrl;

    @Field(type = FieldType.Boolean)
    private Boolean verified;

    @Field(type = FieldType.Integer)
    private Integer viewsCount;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX||uuuu-MM-dd'T'HH:mm:ssXXX||epoch_millis")
    private String possessionDate;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX||uuuu-MM-dd'T'HH:mm:ssXXX||epoch_millis")
    private String indexedAt;
}
