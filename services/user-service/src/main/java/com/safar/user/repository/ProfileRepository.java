package com.safar.user.repository;

import com.safar.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<UserProfile, UUID> {

    /** userId is the PK, so this is equivalent to findById */
    default Optional<UserProfile> findByUserId(UUID userId) {
        return findById(userId);
    }

    List<UserProfile> findByRole(String role);

    List<UserProfile> findByStarHostTrue();
}
