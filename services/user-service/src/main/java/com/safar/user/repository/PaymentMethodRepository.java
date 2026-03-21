package com.safar.user.repository;

import com.safar.user.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isDefault = false WHERE p.userId = :userId")
    void clearDefaults(UUID userId);
}
