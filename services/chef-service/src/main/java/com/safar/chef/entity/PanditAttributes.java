package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "pandit_attributes", schema = "services")
@DiscriminatorValue("PANDIT")
@PrimaryKeyJoinColumn(name = "service_listing_id")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PanditAttributes extends ServiceListing {

    @Column(length = 40)
    private String tradition;                       // SMARTA, VAISHNAV, SHAIVITE, SINDHI, ARYA_SAMAJ, IYER, IYENGAR, MAITHIL, ...

    @Column(name = "pandit_gotra", length = 60)
    private String panditGotra;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "text_languages", columnDefinition = "varchar(40)[]")
    private List<String> textLanguages;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "shastra_specializations", columnDefinition = "varchar(40)[]")
    private List<String> shastraSpecializations;    // KARMA_KANDA, JYOTISH, VEDIC, TANTRIC, ASTROLOGY

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "puja_types_offered", columnDefinition = "varchar(40)[]")
    private List<String> pujaTypesOffered;          // GRIHA_PRAVESH, SATYANARAYAN, WEDDING, MUNDAN, ...

    @Column(name = "samagri_provided", length = 20)
    private String samagriProvided;                 // ALL, PARTIAL, NONE

    @Column(name = "online_via_video_call")
    private Boolean onlineViaVideoCall;
}
