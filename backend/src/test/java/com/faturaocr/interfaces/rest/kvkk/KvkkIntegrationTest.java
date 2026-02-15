package com.faturaocr.interfaces.rest.kvkk;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KvkkIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    @Autowired
    private InvoiceJpaRepository invoiceJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Company testCompany;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("KVKK Test Company", "9876543210");
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "kvkk@test.com", "Password123!");
        // Need user for consent/anonymization?
        testDataSeeder.seedUser(testCompany.getId(), "privacy@test.com", "Password123!", Role.MANAGER);
    }

    @Test
    void encryption_ShouldStoreTaxNumberEncryptedButReturnDecrypted() {
        String sensitiveTaxNumber = "1234567890";
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-KVKK-ENC");

        // Update to set tax number via repo (or seeder if it supported it)
        InvoiceJpaEntity entity = invoiceJpaRepository.findById(invoice.getId()).orElseThrow();
        entity.setSupplierTaxNumber(sensitiveTaxNumber);
        // Trigger PrePersist/PreUpdate for hashing/encryption
        invoiceJpaRepository.saveAndFlush(entity);

        // 1. Verify Decryption via Repository (Application Layer)
        InvoiceJpaEntity retrieved = invoiceJpaRepository.findById(invoice.getId()).orElseThrow();
        assertThat(retrieved.getSupplierTaxNumber()).isEqualTo(sensitiveTaxNumber);

        // 2. Verify Encryption via Native Query (Database Layer)
        // This confirms it is NOT stored as plain text
        String rawValue = jdbcTemplate.queryForObject(
                "SELECT supplier_tax_number FROM invoices WHERE id = ?",
                String.class,
                invoice.getId());

        assertThat(rawValue).isNotEqualTo(sensitiveTaxNumber);
        assertThat(rawValue).isNotNull();
    }

    @Test
    void hashing_ShouldPopulateHashField() {
        String taxNumber = "1122334455";
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-KVKK-HASH");

        InvoiceJpaEntity entity = invoiceJpaRepository.findById(invoice.getId()).orElseThrow();
        entity.setSupplierTaxNumber(taxNumber);
        invoiceJpaRepository.saveAndFlush(entity);

        // Verify Hash populated
        InvoiceJpaEntity retrieved = invoiceJpaRepository.findById(invoice.getId()).orElseThrow();
        assertThat(retrieved.getSupplierTaxNumberHash()).isNotNull();
        assertThat(retrieved.getSupplierTaxNumberHash()).isNotEqualTo(taxNumber);

        // Use hash to find (Simulation of search)
        // If repo has search method by hash
        // Optional<InvoiceJpaEntity> found =
        // invoiceJpaRepository.findBySupplierTaxNumberHash(retrieved.getSupplierTaxNumberHash());
        // assertThat(found).isPresent();
    }
}
