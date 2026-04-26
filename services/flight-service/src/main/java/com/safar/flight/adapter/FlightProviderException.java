package com.safar.flight.adapter;

public class FlightProviderException extends RuntimeException {

    private final boolean retryable;

    public FlightProviderException(String message) {
        super(message);
        this.retryable = false;
    }

    public FlightProviderException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
    }

    public FlightProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
