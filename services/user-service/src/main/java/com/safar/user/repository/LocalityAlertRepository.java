package com.safar.user.repository;

import com.safar.user.entity.LocalityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LocalityAlertRepository extends JpaRepository<LocalityAlert, UUID> {
    List<LocalityAlert> findByCityIgnoreCaseAndActiveTrue(String city);
    List<LocalityAlert> findByCityIgnoreCaseAndLocalityIgnoreCaseAndActiveTrue(String city, String locality);
    List<LocalityAlert> findByEmailAndActiveTrue(String email);
    List<LocalityAlert> findByUserIdAndActiveTrue(UUID userId);
    long countByActiveTrue();
}
