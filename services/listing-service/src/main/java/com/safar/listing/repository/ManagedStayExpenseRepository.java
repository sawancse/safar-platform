package com.safar.listing.repository;

import com.safar.listing.entity.ManagedStayExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ManagedStayExpenseRepository extends JpaRepository<ManagedStayExpense, UUID> {
    List<ManagedStayExpense> findByContractId(UUID contractId);
}
