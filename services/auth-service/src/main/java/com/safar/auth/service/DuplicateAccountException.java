package com.safar.auth.service;

/** Thrown when signup would create a second account for someone who already has one. */
public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
