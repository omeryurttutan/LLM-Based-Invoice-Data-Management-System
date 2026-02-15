package com.faturaocr.infrastructure.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    // 32-byte key for AES-256
    private static final byte[] KEY_BYTES = "12345678901234567890123456789012".getBytes();
    private static final SecretKey SECRET_KEY = new SecretKeySpec(KEY_BYTES, "AES");

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(SECRET_KEY);
    }

    @Test
    void testEncryptDecrypt() {
        String original = "Hello, KVKK!";
        String encrypted = encryptionService.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptDecryptEmpty() {
        String original = "";
        // Assuming encrypt handles empty string? It uses getBytes(), so empty byte
        // array.
        // GCM usually works.
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptGarbage() {
        String garbage = "garbage_base64_string";
        // decrypt expects Base64. If parsing fails, it might throw
        // IllegalArgumentException from Base64 decoder
        // OR general exception caught by decrypt -> returns [ENCRYPTED]

        // EncryptionService catches Exception and returns "[ENCRYPTED]"
        String decrypted = encryptionService.decrypt(garbage);
        assertEquals("[ENCRYPTED]", decrypted);
    }
}
