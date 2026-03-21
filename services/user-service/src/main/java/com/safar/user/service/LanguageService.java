package com.safar.user.service;

import com.safar.user.entity.UserProfile;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LanguageService {

    public static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en", "hi", "te", "ta", "mr", "bn", "gu"
    );

    private final ProfileRepository profileRepository;

    @Transactional
    public String updateLanguage(UUID userId, String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException(
                    "Unsupported language: " + language + ". Supported: " + SUPPORTED_LANGUAGES);
        }
        UserProfile profile = profileRepository.findById(userId)
                .orElse(UserProfile.builder().userId(userId).build());
        profile.setPreferredLanguage(language);
        profileRepository.save(profile);
        return language;
    }
}
