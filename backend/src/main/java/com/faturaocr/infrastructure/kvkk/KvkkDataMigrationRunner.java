package com.faturaocr.infrastructure.kvkk;

import com.faturaocr.infrastructure.security.encryption.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KvkkDataMigrationRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KvkkDataMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionService encryptionService;

    public KvkkDataMigrationRunner(JdbcTemplate jdbcTemplate, EncryptionService encryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (isMigrationNeeded()) {
            LOGGER.info("Starting KVKK sensitive data migration...");
            migrateUsers();
            migrateCompanies();
            migrateInvoices();
            migrateSupplierTemplates();
            markMigrationComplete();
            LOGGER.info("KVKK sensitive data migration completed successfully.");
        } else {
            LOGGER.debug("KVKK data migration already completed or not needed.");
        }
    }

    private boolean isMigrationNeeded() {
        try {
            String sql = "SELECT value FROM system_settings WHERE key = 'data_encrypted'";
            String value = jdbcTemplate.queryForObject(sql, String.class);
            return "false".equalsIgnoreCase(value);
        } catch (Exception e) {
            LOGGER.warn("Could not check migration status: {}", e.getMessage());
            // If table doesn't exist or row missing, maybe assume needed or fail.
            // Since V32 creates it, it should exist. Fail safe: don't migrate if unsure to
            // avoid double encryption.
            return false;
        }
    }

    private void migrateUsers() {
        LOGGER.info("Migrating Users...");
        String selectSql = "SELECT id, phone FROM users WHERE phone IS NOT NULL";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);

        for (Map<String, Object> row : rows) {
            UUID id = (UUID) row.get("id");
            String phone = (String) row.get("phone");

            if (phone != null && !isEncrypted(phone)) {
                String encryptedPhone = encryptionService.encrypt(phone);
                jdbcTemplate.update("UPDATE users SET phone = ? WHERE id = ?", encryptedPhone, id);
            }
        }
        LOGGER.info("Migrated {} users.", rows.size());
    }

    private void migrateCompanies() {
        LOGGER.info("Migrating Companies...");
        String selectSql = "SELECT id, tax_number, address, phone FROM companies";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);

        for (Map<String, Object> row : rows) {
            UUID id = (UUID) row.get("id");
            String taxNumber = (String) row.get("tax_number");
            String address = (String) row.get("address");
            String phone = (String) row.get("phone");

            String encryptedTaxCheck = taxNumber;
            String encryptedAddress = address;
            String encryptedPhone = phone;
            String taxNumberHash = null;

            boolean updated = false;

            if (taxNumber != null && !isEncrypted(taxNumber)) {
                encryptedTaxCheck = encryptionService.encrypt(taxNumber);
                taxNumberHash = hashString(taxNumber);
                updated = true;
            }
            if (address != null && !isEncrypted(address)) {
                encryptedAddress = encryptionService.encrypt(address);
                updated = true;
            }
            if (phone != null && !isEncrypted(phone)) {
                encryptedPhone = encryptionService.encrypt(phone);
                updated = true;
            }

            if (updated) {
                if (taxNumberHash != null) {
                    jdbcTemplate.update(
                            "UPDATE companies SET tax_number = ?, address = ?, phone = ?, tax_number_hash = ? WHERE id = ?",
                            encryptedTaxCheck, encryptedAddress, encryptedPhone, taxNumberHash, id);
                } else {
                    jdbcTemplate.update("UPDATE companies SET tax_number = ?, address = ?, phone = ? WHERE id = ?",
                            encryptedTaxCheck, encryptedAddress, encryptedPhone, id);
                }
            }
        }
        LOGGER.info("Migrated {} companies.", rows.size());
    }

    private void migrateInvoices() {
        LOGGER.info("Migrating Invoices...");
        // This is potentially large. Should paginate!
        // But for MVP/Graduation project, full scan might be ok if dataset is small.
        // Prompt says "This should run in batches (e.g., 100 records at a time)".
        // Implementing simple pagination.

        int pageSize = 100;
        long count = jdbcTemplate.queryForObject("SELECT count(*) FROM invoices", Long.class);

        for (int i = 0; i < count; i += pageSize) {
            String selectSql = "SELECT id, supplier_tax_number FROM invoices ORDER BY id LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql, pageSize, i);

            for (Map<String, Object> row : rows) {
                UUID id = (UUID) row.get("id");
                String supplierTax = (String) row.get("supplier_tax_number");

                if (supplierTax != null && !isEncrypted(supplierTax)) {
                    String encrypted = encryptionService.encrypt(supplierTax);
                    String hash = hashString(supplierTax);
                    jdbcTemplate.update(
                            "UPDATE invoices SET supplier_tax_number = ?, supplier_tax_number_hash = ? WHERE id = ?",
                            encrypted, hash, id);
                }
            }
        }
        LOGGER.info("Migrated {} invoices.", count);
    }

    private void migrateSupplierTemplates() {
        LOGGER.info("Migrating Supplier Templates...");
        String selectSql = "SELECT id, supplier_tax_number FROM supplier_templates WHERE supplier_tax_number IS NOT NULL";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);

        for (Map<String, Object> row : rows) {
            UUID id = (UUID) row.get("id");
            String taxNumber = (String) row.get("supplier_tax_number");

            if (taxNumber != null && !isEncrypted(taxNumber)) {
                String encrypted = encryptionService.encrypt(taxNumber);
                String hash = hashString(taxNumber);
                jdbcTemplate.update(
                        "UPDATE supplier_templates SET supplier_tax_number = ?, supplier_tax_number_hash = ? WHERE id = ?",
                        encrypted, hash, id);
            }
        }
        LOGGER.info("Migrated {} supplier templates.", rows.size());
    }

    private void markMigrationComplete() {
        jdbcTemplate.update("UPDATE system_settings SET value = 'true' WHERE key = 'data_encrypted'");
    }

    private boolean isEncrypted(String value) {
        // Try to decrypt. If it fails or returns error indicator, it's not encrypted
        // properly.
        // Wait, current decrypt implementation returns "[ENCRYPTED]" if decryption
        // fails.
        // If I pass plain text "123", decrypt("123") -> base64 decode fails or tag
        // mismatch -> returns "[ENCRYPTED]".
        // So checking if result equals "[ENCRYPTED]" means input is likely NOT valid
        // encrypted string (OR corrupted).
        // If it was valid encrypted string, decrypt would define it.
        // BUT wait. If "123" is treated as ciphertext, and decrypt fails, the service
        // returns indicator.
        // The service returns "[ENCRYPTED]" (meaning: failed to decrypt, showing
        // placeholder).
        // This logic is tricky.

        // Simpler check: check if it's Base64 AND has enough length for IV + Tag.
        // But plain text might be base64.

        // Let's rely on decrypt return value.
        // If decrypt(val) returns "[ENCRYPTED]" -> it failed to decrypt -> likely plain
        // text (or garbage).
        // If decrypt(val) returns something else (success) -> it WAS encrypted.

        // CORRECTION:
        // Service.decrypt() logic:
        // catch (Exception e) -> return "[ENCRYPTED]";
        // If input is Plain Text, it will likely fail base64 or GCM tag check ->
        // Exception -> returns "[ENCRYPTED]".
        // So validation logic:
        // if (decrypt(val).equals("[ENCRYPTED]")) -> Input is NOT encrypted (Plain
        // Text).
        // else -> Input IS encrypted.

        // EDGE CASE: What if the plain text IS "[ENCRYPTED]"? Unlikely.

        String decrypted = encryptionService.decrypt(value);
        return !"[ENCRYPTED]".equals(decrypted) && !"[DECRYPTION_FAILED]".equals(decrypted);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
