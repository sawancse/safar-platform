package com.safar.listing.repository;

import com.safar.listing.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, UUID> {

    List<LoanDocument> findByLoanApplicationId(UUID loanApplicationId);
}
