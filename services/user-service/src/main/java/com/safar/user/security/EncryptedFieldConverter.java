package com.safar.user.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts String fields
 * using AES-256-GCM. Apply to any entity column with @Convert(converter = ...).
 *
 * Gracefully handles unencrypted legacy data (returns as-is on decrypt failure).
 */
@Converter
@Component
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private final AES256Encryptor encryptor;

    public EncryptedFieldConverter(AES256Encryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
