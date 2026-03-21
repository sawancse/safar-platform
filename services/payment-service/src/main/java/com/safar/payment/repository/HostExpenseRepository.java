package com.safar.payment.repository;

import com.safar.payment.entity.HostExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HostExpenseRepository extends JpaRepository<HostExpense, UUID> {
    List<HostExpense> findByHostId(UUID hostId);
    List<HostExpense> findByHostIdAndExpenseDateBetween(UUID hostId, java.time.LocalDate start, java.time.LocalDate end);
}
