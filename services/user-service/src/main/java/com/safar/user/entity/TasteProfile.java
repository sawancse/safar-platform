package com.safar.user.entity;

import com.safar.user.config.ListToArrayConverter;
import com.safar.user.entity.enums.BudgetTier;
import com.safar.user.entity.enums.GroupType;
import com.safar.user.entity.enums.PropertyVibe;
import com.safar.user.entity.enums.TravelStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "taste_profiles", schema = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TasteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style")
    private TravelStyle travelStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_vibe")
    private PropertyVibe propertyVibe;

    /**
     * Stored as a PostgreSQL TEXT[] in production.
     * The {@link ListToArrayConverter} serialises the list to/from the
     * {@code {"item1","item2"}} array-literal format so the mapping is
     * transparent to both PostgreSQL and H2 (used in tests).
     */
    @Convert(converter = ListToArrayConverter.class)
    @Column(name = "must_haves", columnDefinition = "TEXT")
    private List<String> mustHaves;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_tier")
    private BudgetTier budgetTier;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
