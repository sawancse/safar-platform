package com.safar.auth.repository;

import com.safar.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByAppleId(String appleId);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
