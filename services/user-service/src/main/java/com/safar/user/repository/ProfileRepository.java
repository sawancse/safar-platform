package com.safar.user.repository;

import com.safar.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<UserProfile, UUID>,
        JpaSpecificationExecutor<UserProfile> {

    /** userId is the PK, so this is equivalent to findById */
    default Optional<UserProfile> findByUserId(UUID userId) {
        return findById(userId);
    }

    List<UserProfile> findByRole(String role);

    List<UserProfile> findByStarHostTrue();

    long countByCreatedAtAfter(OffsetDateTime after);

    @Query("SELECT p.role, COUNT(p) FROM UserProfile p GROUP BY p.role")
    List<Object[]> countByRoleGrouped();
}
