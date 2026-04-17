package com.safar.booking.repository;

import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenancyInvoiceRepository extends JpaRepository<TenancyInvoice, UUID> {

    Page<TenancyInvoice> findByTenancyIdOrderByBillingYearDescBillingMonthDesc(UUID tenancyId, Pageable pageable);

    List<TenancyInvoice> findByStatus(InvoiceStatus status);

    boolean existsByTenancyIdAndBillingMonthAndBillingYear(UUID tenancyId, int month, int year);

    @Query("SELECT MAX(i.invoiceNumber) FROM TenancyInvoice i WHERE i.invoiceNumber LIKE :prefix")
    Optional<String> findMaxInvoiceNumberLike(@Param("prefix") String prefix);

    List<TenancyInvoice> findByDueDateAndStatusInAndReminder5dSentFalse(LocalDate dueDate, List<InvoiceStatus> statuses);

    List<TenancyInvoice> findByDueDateAndStatusInAndReminder1dSentFalse(LocalDate dueDate, List<InvoiceStatus> statuses);
}
