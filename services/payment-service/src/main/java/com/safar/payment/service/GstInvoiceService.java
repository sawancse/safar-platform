package com.safar.payment.service;

import com.safar.payment.dto.ExpenseRequest;
import com.safar.payment.dto.GstBreakdown;
import com.safar.payment.dto.PnlStatement;
import com.safar.payment.dto.TaxProfileRequest;
import com.safar.payment.dto.TdsReport;
import com.safar.payment.entity.GstInvoice;
import com.safar.payment.entity.HostExpense;
import com.safar.payment.entity.HostTaxProfile;
import com.safar.payment.repository.GstInvoiceRepository;
import com.safar.payment.repository.HostExpenseRepository;
import com.safar.payment.repository.HostTaxProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class GstInvoiceService {

    private final HostTaxProfileRepository taxProfileRepo;
    private final HostExpenseRepository expenseRepo;
    private final GstInvoiceRepository gstInvoiceRepo;

    private final AtomicLong invoiceCounter = new AtomicLong(0);

    @Transactional
    public HostTaxProfile createTaxProfile(UUID hostId, TaxProfileRequest req) {
        HostTaxProfile profile = HostTaxProfile.builder()
                .hostId(hostId)
                .gstin(req.gstin())
                .pan(req.pan())
                .businessName(req.businessName())
                .stateCode(req.stateCode())
                .build();
        return taxProfileRepo.save(profile);
    }

    public HostTaxProfile getTaxProfile(UUID hostId) {
        return taxProfileRepo.findByHostId(hostId)
                .orElseThrow(() -> new NoSuchElementException("Tax profile not found for host: " + hostId));
    }

    @Transactional
    public HostExpense logExpense(UUID hostId, ExpenseRequest req) {
        HostExpense expense = HostExpense.builder()
                .hostId(hostId)
                .listingId(req.listingId())
                .category(req.category())
                .amountPaise(req.amountPaise())
                .gstPaise(req.gstPaise() != null ? req.gstPaise() : 0L)
                .description(req.description())
                .expenseDate(req.expenseDate())
                .build();
        return expenseRepo.save(expense);
    }

    public List<HostExpense> getExpenses(UUID hostId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return expenseRepo.findByHostIdAndExpenseDateBetween(hostId, start, end);
    }

    public List<GstInvoice> getGstInvoices(UUID hostId) {
        return gstInvoiceRepo.findByHostId(hostId);
    }

    @Transactional
    public GstInvoice generateInvoice(UUID hostId, UUID bookingId, long totalAmountPaise, boolean isInterState) {
        // For 18% GST: totalAmount = taxableAmount * 1.18
        // taxableAmount = totalAmount * 100 / 118
        long taxableAmount = Math.round(totalAmountPaise * 100.0 / 118.0);
        long gstAmount = totalAmountPaise - taxableAmount;

        long cgstAmount = 0;
        long sgstAmount = 0;
        long igstAmount = 0;

        if (isInterState) {
            igstAmount = gstAmount;
        } else {
            cgstAmount = gstAmount / 2;
            sgstAmount = gstAmount - cgstAmount; // handle odd paise
        }

        String invoiceNumber = generateInvoiceNumber();

        GstInvoice invoice = GstInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .hostId(hostId)
                .bookingId(bookingId)
                .taxableAmount(taxableAmount)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .totalAmount(totalAmountPaise)
                .invoiceDate(LocalDate.now())
                .build();

        log.info("Generated GST invoice {} for host {} booking {}", invoiceNumber, hostId, bookingId);
        return gstInvoiceRepo.save(invoice);
    }

    public TdsReport generateTdsReport(UUID hostId, int year, int quarter) {
        HostTaxProfile profile = getTaxProfile(hostId);

        // Quarter date range
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = YearMonth.of(year, endMonth).atEndOfMonth();

        List<GstInvoice> invoices = gstInvoiceRepo.findByHostIdAndInvoiceDateBetween(hostId, start, end);

        long totalRevenue = invoices.stream()
                .mapToLong(GstInvoice::getTaxableAmount)
                .sum();

        // TDS @1% of revenue
        long tdsDeducted = Math.round(totalRevenue * 0.01);

        String period = String.format("Q%d-%d", quarter, year);

        return new TdsReport(hostId, profile.getPan(), period, totalRevenue, tdsDeducted, invoices.size());
    }

    public PnlStatement generatePnl(UUID hostId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        // Revenue from GST invoices
        List<GstInvoice> invoices = gstInvoiceRepo.findByHostIdAndInvoiceDateBetween(hostId, start, end);
        long grossRevenue = invoices.stream()
                .mapToLong(GstInvoice::getTaxableAmount)
                .sum();

        // Expenses
        List<HostExpense> expenses = expenseRepo.findByHostIdAndExpenseDateBetween(hostId, start, end);
        long totalExpenses = expenses.stream()
                .mapToLong(HostExpense::getAmountPaise)
                .sum();

        // Platform fees at 3% of revenue
        long platformFees = Math.round(grossRevenue * 0.03);

        // TDS at 1% of revenue
        long tdsDeducted = Math.round(grossRevenue * 0.01);

        long netProfit = grossRevenue - totalExpenses - platformFees - tdsDeducted;

        return new PnlStatement(hostId, year, grossRevenue, totalExpenses, platformFees, tdsDeducted, netProfit);
    }

    /**
     * Slab-based GST calculation per India hospitality GST rules.
     * Rates: exempt below Rs 1,000/night, 12% for Rs 1,000-7,500, 18% above Rs 7,500.
     * Medical bookings get flat 5%.
     */
    public GstBreakdown calculateGst(long perNightPaise, int nights, String guestState,
                                      String propertyState, String bookingType) {
        double gstRate;
        if ("MEDICAL".equals(bookingType)) {
            gstRate = 0.05; // 5% for healthcare-adjacent
        } else if (perNightPaise < 100000) { // < Rs 1,000/night
            gstRate = 0.0; // Exempt
        } else if (perNightPaise <= 750000) { // Rs 1,000 - 7,500/night
            gstRate = 0.12;
        } else { // > Rs 7,500/night
            gstRate = 0.18;
        }

        long taxableAmount = perNightPaise * nights;
        long gstAmount = Math.round(taxableAmount * gstRate);

        boolean isInterState = !guestState.equalsIgnoreCase(propertyState);

        long cgst = 0, sgst = 0, igst = 0;
        if (gstRate > 0) {
            if (isInterState) {
                igst = gstAmount;
            } else {
                cgst = gstAmount / 2;
                sgst = gstAmount - cgst; // handles odd paise
            }
        }

        return new GstBreakdown(taxableAmount, gstRate, cgst, sgst, igst, gstAmount, taxableAmount + gstAmount);
    }

    private String generateInvoiceNumber() {
        YearMonth now = YearMonth.now();
        long seq = invoiceCounter.incrementAndGet();
        return String.format("SAF-GST-%d%02d-%04d", now.getYear(), now.getMonthValue(), seq);
    }
}
