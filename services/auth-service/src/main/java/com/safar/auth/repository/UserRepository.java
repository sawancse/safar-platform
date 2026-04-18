package com.safar.auth.repository;

import com.safar.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);

    /** Case-insensitive email lookup — pairs with the LOWER(email) partial unique
     *  index added in V11 so mixed-case rows don't leak duplicate accounts. */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByAppleId(String appleId);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
