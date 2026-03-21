package com.safar.user.service;

import com.safar.user.dto.TasteProfileDto;
import com.safar.user.dto.UpdateTasteProfileRequest;
import com.safar.user.entity.TasteProfile;
import com.safar.user.repository.TasteProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final TasteProfileRepository tasteProfileRepository;

    /**
     * Returns the taste profile for the given user.
     *
     * @throws NoSuchElementException if no profile exists yet for that user
     */
    public TasteProfileDto getProfile(UUID userId) {
        TasteProfile profile = tasteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("No taste profile found for user " + userId));
        return toDto(profile);
    }

    /**
     * Creates a new taste profile if one does not exist, or replaces all
     * fields on the existing one. All fields are updated unconditionally
     * (including setting them to {@code null}) so the caller must supply
     * the full desired state on every request.
     */
    @Transactional
    public TasteProfileDto upsertProfile(UUID userId, UpdateTasteProfileRequest req) {
        TasteProfile profile = tasteProfileRepository.findByUserId(userId)
                .orElse(TasteProfile.builder().userId(userId).build());

        profile.setTravelStyle(req.travelStyle());
        profile.setPropertyVibe(req.propertyVibe());
        profile.setMustHaves(req.mustHaves());
        profile.setGroupType(req.groupType());
        profile.setBudgetTier(req.budgetTier());

        return toDto(tasteProfileRepository.save(profile));
    }

    private TasteProfileDto toDto(TasteProfile p) {
        return new TasteProfileDto(
                p.getId(), p.getUserId(),
                p.getTravelStyle(), p.getPropertyVibe(),
                p.getMustHaves(), p.getGroupType(),
                p.getBudgetTier(), p.getUpdatedAt()
        );
    }
}
