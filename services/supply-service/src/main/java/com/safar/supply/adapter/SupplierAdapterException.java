package com.safar.supply.adapter;

/**
 * Thrown by a SupplierAdapter when an external API call fails.
 * Caller (PurchaseOrderService, scheduled jobs) decides retry policy
 * based on isRetryable().
 */
public class SupplierAdapterException extends RuntimeException {

    private final boolean retryable;

    public SupplierAdapterException(String msg, boolean retryable) {
        super(msg);
        this.retryable = retryable;
    }

    public SupplierAdapterException(String msg, Throwable cause, boolean retryable) {
        super(msg, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
