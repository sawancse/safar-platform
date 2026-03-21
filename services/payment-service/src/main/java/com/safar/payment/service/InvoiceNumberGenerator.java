package com.safar.payment.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InvoiceNumberGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);

    public String next() {
        return String.format("SAF-INV-%06d", counter.incrementAndGet());
    }
}
