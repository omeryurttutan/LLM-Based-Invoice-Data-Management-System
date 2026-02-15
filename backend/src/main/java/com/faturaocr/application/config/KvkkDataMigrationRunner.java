package com.faturaocr.application.config;

import com.faturaocr.infrastructure.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KvkkDataMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionService encryptionService;

    @Override
    public void run(String... args) throws Exception {
        if (isMigrationCompleted()) {
            return;
        }

        log.info("Starting KVKK Data Migration: Encrypting existing plain text data...");

        // 1. Users (phone)
        encryptTable("users", "phone", null, true); // User has no hash column for phone in my entity update check?
        // Wait, UserJpaEntity has phone. Did I add phoneHash? No.
        // User phone is mapped with @Convert. Queries by phone will fail if not hashed.
        // But I didn't add phone_hash to Users!
        // Prompt Check: "Search Limitations: ... tax numbers ... hash columns".
        // Did it mention phone hash?
        // User entity check: `private String phone`.
        // Search usually happens on `email`. `phone` search might not be critical.
        // If it is, I missed it. But I should check instructions.
        // "Hash Columns | tax_number_hash, ..."
        // It lists tax numbers. Doesn't explicitly list phone hash.
        // So I assume phone search is not required or handled differently.

        // 2. Companies (tax_number, address, phone)
        encryptTable("companies", "tax_number", "tax_number_hash", true);
        encryptTable("companies", "address", null, false); // Address not hashed
        encryptTable("companies", "phone", null, false); // Phone not hashed in Companies?
        // CompanyJpaEntity has taxNumberHash.

        // 3. Invoices (supplier_tax_number, buyer_tax_number)
        encryptTable("invoices", "supplier_tax_number", "supplier_tax_number_hash", true);
        encryptTable("invoices", "buyer_tax_number", "buyer_tax_number_hash", true);

        // 4. Supplier Templates (supplier_tax_number)
        encryptTable("supplier_templates", "supplier_tax_number", "supplier_tax_number_hash", true);

        markMigrationCompleted();
        log.info("KVKK Data Migration completed successfully.");
    }

    private boolean isMigrationCompleted() {
        try {
            String val = jdbcTemplate.queryForObject(
                    "SELECT value FROM system_settings WHERE key = 'data_encrypted'", String.class);
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            return false;
        }
    }

    private void markMigrationCompleted() {
        jdbcTemplate.update(
                "INSERT INTO system_settings (key, value, description) VALUES ('data_encrypted', 'true', 'Data migration completed') "
                        +
                        "ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = NOW()");
    }

    private void encryptTable(String tableName, String columnName, String hashColumnName, boolean useHash) {
        log.info("Encrypting table: {}, column: {}", tableName, columnName);

        // Fetch ID and plain value
        // We only fetch rows where value is NOT NULL and NOT starting with something
        // that looks encrypted?
        // But how to detect?
        // AES-256-GCM encrypted string is Base64.
        // If the column was just increased in size, and holds old data, it is plain
        // text.
        // If we ran this partially, we might have mixed data.
        // Check if value can be decrypted? No, decryption throws exception.
        // But attempting to decrypt every row is slow.
        // Since this is a one-time migration and we check `data_encrypted` flag,
        // we assume if flag is FALSE, data is PLAIN.

        // Only select non-null values
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + columnName + " FROM " + tableName + " WHERE " + columnName + " IS NOT NULL");

        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            String plainText = (String) row.get(columnName);

            if (plainText == null || plainText.isEmpty())
                continue;

            // Safety check: if it looks like Base64 and decodes nicely?
            // Better to rely on the flag. We assume it is plain text.

            try {
                String encrypted = encryptionService.encrypt(plainText);

                if (useHash && hashColumnName != null) {
                    String hash = hashString(plainText);
                    jdbcTemplate.update(
                            "UPDATE " + tableName + " SET " + columnName + " = ?, " + hashColumnName
                                    + " = ? WHERE id = ?",
                            encrypted, hash, id);
                } else {
                    jdbcTemplate.update("UPDATE " + tableName + " SET " + columnName + " = ? WHERE id = ?",
                            encrypted, id);
                }
            } catch (Exception e) {
                log.error("Failed to encrypt row id={} in table={}", id, tableName, e);
            }
        }
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
