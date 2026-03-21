package com.safar.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryptor for sensitive PII fields (Aadhaar, PAN, bank account).
 *
 * Format: Base64( IV[12] || ciphertext || authTag[16] )
 *
 * Each encryption uses a unique random IV, so the same plaintext produces
 * different ciphertext every time — safe for deterministic fields like Aadhaar.
 */
@Component
public class AES256Encryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;       // 96-bit IV (GCM recommended)
    private static final int TAG_LENGTH_BITS = 128; // 128-bit authentication tag

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AES256Encryptor(@Value("${encryption.aes-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256 requires a 256-bit (32-byte) key, got " + keyBytes.length + " bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypt plaintext to Base64-encoded ciphertext.
     * Returns null for null input (nullable DB columns).
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: IV || ciphertext+tag
            byte[] combined = ByteBuffer.allocate(IV_LENGTH + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt Base64-encoded ciphertext to plaintext.
     * Returns null for null input. Returns raw value if not Base64 (migration safety).
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            if (combined.length < IV_LENGTH + 1) {
                // Too short to be encrypted — return as-is (unencrypted legacy data)
                return ciphertext;
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            buffer.get(iv);
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not valid Base64 — likely unencrypted legacy data
            return ciphertext;
        } catch (Exception e) {
            // Decryption failed — could be unencrypted legacy data
            return ciphertext;
        }
    }
}
