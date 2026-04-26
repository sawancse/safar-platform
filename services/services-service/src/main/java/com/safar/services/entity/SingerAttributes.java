package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "singer_attributes", schema = "services")
@DiscriminatorValue("SINGER")
@PrimaryKeyJoinColumn(name = "service_listing_id")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SingerAttributes extends ServiceListing {

    @Column(name = "act_type", length = 20)
    private String actType;                     // SOLO, DUO, BAND, TROUPE

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(40)[]")
    private List<String> genres;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(40)[]")
    private List<String> languages;

    @Column(name = "troupe_size_min")
    private Integer troupeSizeMin;

    @Column(name = "troupe_size_max")
    private Integer troupeSizeMax;

    @Column(name = "religious_capable")
    private Boolean religiousCapable;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "audio_reels", columnDefinition = "text[]")
    private List<String> audioReels;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "video_reels", columnDefinition = "text[]")
    private List<String> videoReels;

    @Column(name = "equipment_owned", length = 20)
    private String equipmentOwned;              // FULL_PA, PARTIAL, NONE

    @Column(name = "setup_time_minutes")
    private Integer setupTimeMinutes;
}
