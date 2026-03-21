package com.safar.payment.service;

import com.safar.payment.dto.TdsReport;
import com.safar.payment.entity.GstInvoice;
import com.safar.payment.entity.HostTaxProfile;
import com.safar.payment.repository.GstInvoiceRepository;
import com.safar.payment.repository.HostExpenseRepository;
import com.safar.payment.repository.HostTaxProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GstInvoiceServiceTest {

    @Mock HostTaxProfileRepository taxProfileRepo;
    @Mock HostExpenseRepository expenseRepo;
    @Mock GstInvoiceRepository gstInvoiceRepo;

    @InjectMocks GstInvoiceService gstInvoiceService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @Test
    void generateInvoice_intraState_splitsCgstSgst() {
        // Total = 11800 paise (₹118 inclusive of 18% GST)
        // Taxable = 11800 * 100 / 118 = 10000 paise
        // GST = 1800 paise, CGST = 900, SGST = 900
        long totalAmountPaise = 11800L;

        when(gstInvoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GstInvoice invoice = gstInvoiceService.generateInvoice(hostId, bookingId, totalAmountPaise, false);

        assertThat(invoice.getTaxableAmount()).isEqualTo(10000L);
        assertThat(invoice.getCgstAmount()).isEqualTo(900L);
        assertThat(invoice.getSgstAmount()).isEqualTo(900L);
        assertThat(invoice.getIgstAmount()).isEqualTo(0L);
        assertThat(invoice.getTotalAmount()).isEqualTo(11800L);
        assertThat(invoice.getInvoiceNumber()).startsWith("SAF-GST-");
    }

    @Test
    void generateInvoice_interState_setsIgst() {
        // Total = 11800 paise inclusive of 18% GST
        // Taxable = 10000 paise, IGST = 1800 paise
        long totalAmountPaise = 11800L;

        when(gstInvoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GstInvoice invoice = gstInvoiceService.generateInvoice(hostId, bookingId, totalAmountPaise, true);

        assertThat(invoice.getTaxableAmount()).isEqualTo(10000L);
        assertThat(invoice.getCgstAmount()).isEqualTo(0L);
        assertThat(invoice.getSgstAmount()).isEqualTo(0L);
        assertThat(invoice.getIgstAmount()).isEqualTo(1800L);
        assertThat(invoice.getTotalAmount()).isEqualTo(11800L);
        assertThat(invoice.getInvoiceNumber()).startsWith("SAF-GST-");
    }

    @Test
    void generateTdsReport_calculatesOnePercentTds() {
        HostTaxProfile profile = HostTaxProfile.builder()
                .hostId(hostId)
                .pan("ABCDE1234F")
                .build();
        when(taxProfileRepo.findByHostId(hostId)).thenReturn(Optional.of(profile));

        // Two invoices in Q1 with taxable amounts 100000 and 200000 paise
        GstInvoice inv1 = GstInvoice.builder()
                .hostId(hostId).taxableAmount(100000L).invoiceDate(LocalDate.of(2025, 1, 15)).build();
        GstInvoice inv2 = GstInvoice.builder()
                .hostId(hostId).taxableAmount(200000L).invoiceDate(LocalDate.of(2025, 3, 10)).build();

        when(gstInvoiceRepo.findByHostIdAndInvoiceDateBetween(
                hostId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)))
                .thenReturn(List.of(inv1, inv2));

        TdsReport report = gstInvoiceService.generateTdsReport(hostId, 2025, 1);

        assertThat(report.totalRevenuePaise()).isEqualTo(300000L);
        // TDS @1% = 3000 paise
        assertThat(report.tdsDeductedPaise()).isEqualTo(3000L);
        assertThat(report.invoiceCount()).isEqualTo(2);
        assertThat(report.pan()).isEqualTo("ABCDE1234F");
        assertThat(report.period()).isEqualTo("Q1-2025");
    }
}
