package com.safar.user.repository;

import com.safar.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<UserProfile, UUID> {

    List<UserProfile> findByRole(String role);

    List<UserProfile> findByStarHostTrue();
}
