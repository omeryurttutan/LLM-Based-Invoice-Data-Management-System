package com.faturaocr;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    @Test
    void testPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        System.out.println("VALID_HASH_START|" + encoder.encode("Admin123!") + "|VALID_HASH_END");
    }
}
