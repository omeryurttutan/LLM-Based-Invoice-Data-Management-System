package com.faturaocr.testutil;

import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TestDataBuilder {

    public static InvoiceBuilder anInvoice() {
        return new InvoiceBuilder();
    }

    public static InvoiceItemBuilder anInvoiceItem() {
        return new InvoiceItemBuilder();
    }

    public static CompanyBuilder aCompany() {
        return new CompanyBuilder();
    }

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public static class InvoiceBuilder {
        private final Invoice invoice = new Invoice();

        public InvoiceBuilder() {
            invoice.setId(UUID.randomUUID());
            invoice.setInvoiceNumber("FTR-2024-001");
            invoice.setSupplierName("Test Supplier Ltd");
            invoice.setSupplierTaxNumber("1234567890");
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setCurrency(Currency.TRY);
            invoice.setExchangeRate(BigDecimal.ONE);
        }

        public InvoiceBuilder withId(UUID id) {
            invoice.setId(id);
            return this;
        }

        public InvoiceBuilder withCompanyId(UUID companyId) {
            invoice.setCompanyId(companyId);
            return this;
        }

        public InvoiceBuilder withInvoiceNumber(String invoiceNumber) {
            invoice.setInvoiceNumber(invoiceNumber);
            return this;
        }

        public InvoiceBuilder withSupplierName(String supplierName) {
            invoice.setSupplierName(supplierName);
            return this;
        }

        public InvoiceBuilder withStatus(InvoiceStatus status) {
            invoice.setStatus(status);
            return this;
        }

        public InvoiceBuilder withTotalAmount(BigDecimal amount) {
            invoice.setTotalAmount(amount);
            return this;
        }

        public InvoiceBuilder withItems(InvoiceItem... items) {
            for (InvoiceItem item : items) {
                invoice.addItem(item);
            }
            return this;
        }

        public Invoice build() {
            return invoice;
        }
    }

    public static class InvoiceItemBuilder {
        private final InvoiceItem item = new InvoiceItem();

        public InvoiceItemBuilder() {
            item.setId(UUID.randomUUID());
            item.setDescription("Test Item");
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(new BigDecimal("100.00"));
            item.setTaxRate(new BigDecimal("20.00"));
        }

        public InvoiceItemBuilder withQuantity(BigDecimal quantity) {
            item.setQuantity(quantity);
            return this;
        }

        public InvoiceItemBuilder withUnitPrice(BigDecimal price) {
            item.setUnitPrice(price);
            return this;
        }

        public InvoiceItem build() {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            item.setSubtotal(qty.multiply(price));

            if (item.getTaxRate() != null) {
                BigDecimal tax = item.getSubtotal().multiply(item.getTaxRate()).divide(new BigDecimal("100"));
                item.setTaxAmount(tax);
                item.setTotalAmount(item.getSubtotal().add(tax));
            }

            return item;
        }
    }

    public static class CompanyBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Company";
        private String taxNumber = "1111111111";
        private String email = "info@testcompany.com";
        private boolean isActive = true;

        public CompanyBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public CompanyBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public CompanyBuilder withTaxNumber(String taxNumber) {
            this.taxNumber = taxNumber;
            return this;
        }

        public CompanyBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Company build() {
            return Company.builder()
                    .id(id)
                    .name(name)
                    .taxNumber(taxNumber)
                    .email(email)
                    .isActive(isActive)
                    .defaultCurrency("TRY")
                    .build();
        }
    }

    public static class UserBuilder {
        private UUID id = UUID.randomUUID();
        private UUID companyId = UUID.randomUUID();
        private String email = "user@example.com";
        private String passwordHash = "hashed_password";
        private String fullName = "Test User";
        private Role role = Role.ACCOUNTANT;
        private boolean isActive = true;
        private boolean emailVerified = false;

        public UserBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public UserBuilder withCompanyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withRole(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserBuilder withPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public User build() {
            return User.builder()
                    .id(id)
                    .companyId(companyId)
                    .email(Email.of(email))
                    .passwordHash(passwordHash)
                    .fullName(fullName)
                    .role(role)
                    .isActive(isActive)
                    .emailVerified(emailVerified)
                    .build();
        }
    }
}
