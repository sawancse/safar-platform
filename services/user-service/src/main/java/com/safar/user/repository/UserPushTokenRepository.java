package com.safar.user.repository;

import com.safar.user.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, UUID> {

    List<UserPushToken> findByUserId(UUID userId);

    /** Lookup an existing registration to upsert (avoid dup tokens for same device). */
    Optional<UserPushToken> findByUserIdAndPushToken(UUID userId, String pushToken);

    void deleteByUserIdAndPushToken(UUID userId, String pushToken);
}
