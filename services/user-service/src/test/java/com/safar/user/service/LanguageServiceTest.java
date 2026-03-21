package com.safar.user.service;

import com.safar.user.entity.UserProfile;
import com.safar.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @Mock ProfileRepository profileRepository;
    @InjectMocks LanguageService languageService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void updateLanguage_validLanguage_updatesAndReturns() {
        UserProfile profile = UserProfile.builder().userId(userId).build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = languageService.updateLanguage(userId, "hi");

        assertThat(result).isEqualTo("hi");
        assertThat(profile.getPreferredLanguage()).isEqualTo("hi");
        verify(profileRepository).save(profile);
    }

    @Test
    void updateLanguage_invalidLanguage_throwsIllegalArgument() {
        assertThatThrownBy(() -> languageService.updateLanguage(userId, "fr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported language");
    }
}
