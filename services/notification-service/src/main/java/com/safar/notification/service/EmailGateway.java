package com.safar.notification.service;

/**
 * Abstraction over JavaMailSender for testability.
 */
public interface EmailGateway {

    void send(String to, String subject, String body);
}
