package com.safar.user.repository;

import com.safar.user.entity.BrokerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrokerProfileRepository extends JpaRepository<BrokerProfile, UUID> {

    Optional<BrokerProfile> findByUserId(UUID userId);

    Page<BrokerProfile> findByActiveTrue(Pageable pageable);

    @Query(value = "SELECT * FROM users.broker_profiles WHERE :city = ANY(operating_cities) AND active = true",
            countQuery = "SELECT COUNT(*) FROM users.broker_profiles WHERE :city = ANY(operating_cities) AND active = true",
            nativeQuery = true)
    Page<BrokerProfile> findByCity(@Param("city") String city, Pageable pageable);

    @Query(value = "SELECT * FROM users.broker_profiles WHERE :city = ANY(operating_cities) AND specialization = :spec AND active = true",
            countQuery = "SELECT COUNT(*) FROM users.broker_profiles WHERE :city = ANY(operating_cities) AND specialization = :spec AND active = true",
            nativeQuery = true)
    Page<BrokerProfile> findByCityAndSpecialization(@Param("city") String city, @Param("spec") String spec, Pageable pageable);

    @Query(value = "SELECT * FROM users.broker_profiles WHERE specialization = :spec AND active = true",
            countQuery = "SELECT COUNT(*) FROM users.broker_profiles WHERE specialization = :spec AND active = true",
            nativeQuery = true)
    Page<BrokerProfile> findBySpecialization(@Param("spec") String spec, Pageable pageable);

    boolean existsByUserId(UUID userId);
}
