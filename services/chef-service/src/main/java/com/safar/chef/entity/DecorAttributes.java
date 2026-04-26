package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "decor_attributes", schema = "services")
@DiscriminatorValue("DECORATOR")
@PrimaryKeyJoinColumn(name = "service_listing_id")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DecorAttributes extends ServiceListing {

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "decor_styles", columnDefinition = "varchar(40)[]")
    private List<String> decorStyles;               // PUNJABI, SOUTH_INDIAN, MARWARI, BENGALI, MODERN, ...

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "setup_capabilities", columnDefinition = "varchar(40)[]")
    private List<String> setupCapabilities;         // FLORAL, LIGHTING, STAGE, MANDAP, ...

    @Column(name = "indoor_capable")
    private Boolean indoorCapable;

    @Column(name = "outdoor_capable")
    private Boolean outdoorCapable;

    @Column(name = "largest_event_handled_pax")
    private Integer largestEventHandledPax;

    @Column(name = "crew_size_default")
    private Integer crewSizeDefault;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "equipment_owned", columnDefinition = "text[]")
    private List<String> equipmentOwned;
}
