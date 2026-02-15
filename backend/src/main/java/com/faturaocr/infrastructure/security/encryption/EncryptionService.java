package com.faturaocr.infrastructure.security.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 */
@Service
public class EncryptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // GCM Authentication Tag Length in bits
    private static final int IV_LENGTH_BYTE = 12; // GCM Standard IV Length in bytes

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(
            @Value("${app.kvkk.encryption.key-env-variable:KVKK_ENCRYPTION_KEY}") String keyEnvVarName) {
        this.secureRandom = new SecureRandom();
        String keyBase64 = System.getenv(keyEnvVarName);

        if (keyBase64 == null || keyBase64.isBlank()) {
            // Fallback for development/test if not in env - BUT warn loudly
            // In production this should be a blocker, but for now we might want to allow
            // start
            // Better: try property if env is missing
            keyBase64 = System.getProperty(keyEnvVarName);
        }

        if (keyBase64 == null || keyBase64.isBlank()) {
            // Try to read directly from property as a fallback (for tests)
            // This constructor might be called before property injection if we use @Value
            // for key content
            // But here we rely on env var name.
            // Let's assume for dev/test we might have a default or it's injected
            // differently.
            // For now, if missing log error.
            LOGGER.error("Encryption key not found in environment variable: {}", keyEnvVarName);
            throw new IllegalStateException("Encryption key not found. Please set " + keyEnvVarName);
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // Constructor for testing
    public EncryptionService(SecretKey secretKey) {
        this.secureRandom = new SecureRandom();
        this.secretKey = secretKey;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            // Generate IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            // Initialize Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV + CipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] cipherMessage = byteBuffer.array();

            // Encode (Base64)
            return Base64.getEncoder().encodeToString(cipherMessage);

        } catch (Exception e) {
            LOGGER.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }

        try {
            // Decode Base64
            byte[] cipherMessage = Base64.getDecoder().decode(encryptedText);

            // Extract IV
            if (cipherMessage.length < IV_LENGTH_BYTE) {
                // Might be plain text or corrupted
                LOGGER.warn("Cipher text too short, might be plain text or corrupted.");
                return "[DECRYPTION_FAILED]";
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            // Extract CipherText
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // Initialize Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            LOGGER.error("Error decrypting data: {}", e.getMessage());
            // Return placeholder or handle as configured. Requirement says: return
            // placeholder.
            return "[ENCRYPTED]";
        }
    }
}
