package com.safar.user.service;

import com.safar.user.dto.UpdateTasteProfileRequest;
import com.safar.user.entity.TasteProfile;
import com.safar.user.entity.enums.BudgetTier;
import com.safar.user.entity.enums.GroupType;
import com.safar.user.entity.enums.PropertyVibe;
import com.safar.user.entity.enums.TravelStyle;
import com.safar.user.repository.TasteProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    TasteProfileRepository repository;

    @InjectMocks
    UserProfileService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getProfile_found_returnsDto() {
        TasteProfile profile = TasteProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .travelStyle(TravelStyle.ADVENTURE)
                .budgetTier(BudgetTier.MID)
                .build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        var dto = service.getProfile(userId);

        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.travelStyle()).isEqualTo(TravelStyle.ADVENTURE);
    }

    @Test
    void getProfile_notFound_throwsException() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void upsertProfile_newProfile_creates() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateTasteProfileRequest(
                TravelStyle.CULTURAL, PropertyVibe.COZY,
                List.of("wifi", "pool"), GroupType.FAMILY, BudgetTier.PREMIUM
        );
        var dto = service.upsertProfile(userId, req);

        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.travelStyle()).isEqualTo(TravelStyle.CULTURAL);
        assertThat(dto.mustHaves()).containsExactly("wifi", "pool");
    }

    @Test
    void upsertProfile_existingProfile_updates() {
        TasteProfile existing = TasteProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .travelStyle(TravelStyle.RELAXED)
                .build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateTasteProfileRequest(
                TravelStyle.WELLNESS, null, null, null, BudgetTier.LUXURY
        );
        var dto = service.upsertProfile(userId, req);

        assertThat(dto.travelStyle()).isEqualTo(TravelStyle.WELLNESS);
        assertThat(dto.budgetTier()).isEqualTo(BudgetTier.LUXURY);
    }
}
