package com.safar.insurance.adapter;

public class InsuranceProviderException extends RuntimeException {
    private final boolean retryable;

    public InsuranceProviderException(String message) {
        super(message);
        this.retryable = false;
    }

    public InsuranceProviderException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
    }

    public InsuranceProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
