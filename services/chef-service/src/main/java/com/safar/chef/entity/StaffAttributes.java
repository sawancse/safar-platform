package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "staff_attributes", schema = "services")
@DiscriminatorValue("STAFF_HIRE")
@PrimaryKeyJoinColumn(name = "service_listing_id")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StaffAttributes extends ServiceListing {

    @Column(name = "agency_type", length = 30)
    private String agencyType;                      // INDIVIDUAL_FREELANCER, SMALL_AGENCY, LARGE_AGENCY

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles_offered", columnDefinition = "varchar(40)[]")
    private List<String> rolesOffered;              // WAITER, BARTENDER, SERVER, KITCHEN_HELPER, ...

    @Column(name = "min_count_per_booking")
    private Integer minCountPerBooking;

    @Column(name = "max_count_per_booking")
    private Integer maxCountPerBooking;

    @Column(name = "uniform_provided")
    private Boolean uniformProvided;

    @Column(name = "experience_years_avg")
    private Integer experienceYearsAvg;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages_spoken", columnDefinition = "varchar(40)[]")
    private List<String> languagesSpoken;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dress_codes_supported", columnDefinition = "varchar(40)[]")
    private List<String> dressCodesSupported;       // FORMAL_BLACK, SAREE, KURTA, EVENT_THEME
}
