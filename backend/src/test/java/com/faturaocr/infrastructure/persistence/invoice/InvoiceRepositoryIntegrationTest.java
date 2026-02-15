package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InvoiceJpaRepository invoiceJpaRepository;

    @Autowired
    private TestDataSeeder testDataSeeder;

    // We need validation repository to update manually for some tests if seeder
    // doesn't support specific fields
    // or just use the seedInvoice returns domain and we update via adapter or just
    // seed carefully.

    private Company company;

    @BeforeEach
    void setUp() {
        company = testDataSeeder.seedCompany("Repo Test Company", "9999999999");
    }

    @Test
    void findByIdAndCompanyIdAndIsDeletedFalse_ShouldReturnInvoice() {
        Invoice invoice = testDataSeeder.seedInvoice(company.getId(), "INV-REPO-1");

        Optional<InvoiceJpaEntity> found = invoiceJpaRepository.findByIdAndCompanyIdAndIsDeletedFalse(
                invoice.getId(), company.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceNumber()).isEqualTo("INV-REPO-1");
    }

    @Test
    void findPotentialDuplicates_ShouldReturnMatches() {
        // Seeding directly might be hard via seeder if we need exact control over dates
        // and amounts with precision
        // But seedInvoice returns Invoice, which we can update via InvoiceRepository
        // port?
        // Or just trust seedInvoice defaults and modify?
        // TestDataSeeder.seedInvoice sets status PENDING, date NOW, amount ?, supplier
        // Name ?

        // Let's rely on what we can control.
        // seedInvoice takes number.
        // I'll use the repository to save specific entities for this test to be
        // precise.

        InvoiceJpaEntity entity1 = new InvoiceJpaEntity();
        entity1.setId(UUID.randomUUID());
        entity1.setCompanyId(company.getId());
        entity1.setInvoiceNumber("INV-DUP-1");
        entity1.setSupplierName(" Common Supplier ");
        entity1.setInvoiceDate(LocalDate.now());
        entity1.setTotalAmount(BigDecimal.valueOf(100.00));
        entity1.setCurrency(com.faturaocr.domain.invoice.valueobject.Currency.TRY); // Enum matching?
        // Currency is ValueObject in domain, but Enum in Entity?
        // InvoiceJpaEntity definition:
        // @Enumerated(EnumType.STRING) private Currency currency;
        // Check imports.
        entity1.setCurrency(Currency.TRY);
        entity1.setStatus(InvoiceStatus.PENDING);
        entity1.setDeleted(false);
        invoiceJpaRepository.save(entity1);

        InvoiceJpaEntity entity2 = new InvoiceJpaEntity();
        entity2.setId(UUID.randomUUID());
        entity2.setCompanyId(company.getId());
        entity2.setInvoiceNumber("INV-DUP-2");
        entity2.setSupplierName("common supplier"); // Case insensitive check
        entity2.setInvoiceDate(LocalDate.now());
        entity2.setTotalAmount(BigDecimal.valueOf(100.00));
        entity2.setCurrency(Currency.TRY);
        entity2.setStatus(InvoiceStatus.PENDING);
        entity2.setDeleted(false);
        invoiceJpaRepository.save(entity2);

        List<InvoiceJpaEntity> duplicates = invoiceJpaRepository.findPotentialDuplicatesBySupplierAndDateAndAmountRange(
                "Common Supplier",
                LocalDate.now(),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(110),
                company.getId(),
                entity1.getId() // exclude self
        );

        assertThat(duplicates).hasSize(1);
        assertThat(duplicates.get(0).getInvoiceNumber()).isEqualTo("INV-DUP-2");
    }

    @Test
    void findMinMaxTotalAmount_ShouldReturnCorrectValues() {
        createEntity("INV-MM-1", BigDecimal.valueOf(100));
        createEntity("INV-MM-2", BigDecimal.valueOf(500));
        createEntity("INV-MM-3", BigDecimal.valueOf(50));

        Object[] result = invoiceJpaRepository.findMinMaxTotalAmount(company.getId());

        // result is Object[] { min, max }
        assertThat(result).isNotNull();
        assertThat(result.length).isEqualTo(2);
        assertThat((BigDecimal) result[0]).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat((BigDecimal) result[1]).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    private void createEntity(String number, BigDecimal amount) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(company.getId());
        entity.setInvoiceNumber(number);
        entity.setTotalAmount(amount);
        entity.setSupplierName("Supplier");
        entity.setCurrency(Currency.TRY);
        entity.setStatus(InvoiceStatus.PENDING);
        entity.setInvoiceDate(LocalDate.now());
        entity.setDeleted(false);
        invoiceJpaRepository.save(entity);
    }
}
