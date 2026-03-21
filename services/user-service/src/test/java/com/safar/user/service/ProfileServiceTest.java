package com.safar.user.service;

import com.safar.user.dto.UpdateProfileRequest;
import com.safar.user.entity.UserProfile;
import com.safar.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock ProfileRepository profileRepository;
    @InjectMocks ProfileService profileService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getMyProfile_existing_returnsProfile() {
        UserProfile profile = UserProfile.builder()
                .userId(userId).name("Rahul").email("rahul@example.com").language("en").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        var dto = profileService.getMyProfile(userId);

        assertThat(dto.name()).isEqualTo("Rahul");
        assertThat(dto.email()).isEqualTo("rahul@example.com");
    }

    @Test
    void getMyProfile_notExisting_returnsEmptyProfile() {
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        var dto = profileService.getMyProfile(userId);

        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.name()).isEqualTo("");
    }

    @Test
    void updateProfile_createsNewIfNotExists() {
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = profileService.updateProfile(userId, new UpdateProfileRequest("Priya", null, null, null, "hi", null, null, null, null, null, null, null, null, null, null));

        assertThat(dto.name()).isEqualTo("Priya");
        assertThat(dto.language()).isEqualTo("hi");
    }

    @Test
    void updateProfile_updatesExisting() {
        UserProfile existing = UserProfile.builder()
                .userId(userId).name("OldName").language("en").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = profileService.updateProfile(userId, new UpdateProfileRequest("NewName", null, "new@email.com", null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(dto.name()).isEqualTo("NewName");
        assertThat(dto.email()).isEqualTo("new@email.com");
        assertThat(dto.language()).isEqualTo("en"); // unchanged
    }

    @Test
    void getPublicHostProfile_notFound_throws() {
        when(profileRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profileService.getPublicHostProfile(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
