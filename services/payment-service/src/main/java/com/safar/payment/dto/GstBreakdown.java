package com.safar.payment.dto;

public record GstBreakdown(
    long taxableAmountPaise,
    double gstRate,
    long cgstPaise,
    long sgstPaise,
    long igstPaise,
    long totalGstPaise,
    long grandTotalPaise
) {}
