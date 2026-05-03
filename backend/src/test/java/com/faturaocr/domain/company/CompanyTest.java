package com.faturaocr.domain.company;

import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CompanyTest {

    @Test
    @DisplayName("Should create company with valid data")
    void shouldCreateCompanyWithValidData() {
        // Given
        String name = "Test A.Ş.";
        String taxNumber = "1234567890";

        // When
        Company company = new Company(name, taxNumber);

        // Then
        assertThat(company.getName()).isEqualTo(name);
        assertThat(company.getTaxNumber()).isEqualTo(taxNumber);
        assertThat(company.isActive()).isTrue();
        assertThat(company.getDefaultCurrency()).isEqualTo("TRY");
        assertThat(company.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should create company using builder")
    void shouldCreateCompanyUsingBuilder() {
        // When
        Company company = TestDataBuilder.aCompany()
                .withName("Builder Corp")
                .withTaxNumber("9876543210")
                .build();

        // Then
        assertThat(company.getName()).isEqualTo("Builder Corp");
        assertThat(company.getTaxNumber()).isEqualTo("9876543210");
        assertThat(company.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should validate tax number length")
    void shouldValidateTaxNumberLength() {
        // Given
        String name = "Invalid Ltd";
        String shortTax = "123";
        String longTax = "12345678901";

        // When/Then
        Throwable thrownShort = catchThrowable(() -> new Company(name, shortTax));
        assertThat(thrownShort).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax number must be exactly 10 digits");

        Throwable thrownLong = catchThrowable(() -> new Company(name, longTax));
        assertThat(thrownLong).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax number must be exactly 10 digits");
    }

    @Test
    @DisplayName("Should validate tax number numeric")
    void shouldValidateTaxNumberNumeric() {
        // Given
        String name = "Alpha Ltd";
        String nonNumericTax = "12345ABC90";

        // When/Then
        Throwable thrown = catchThrowable(() -> new Company(name, nonNumericTax));
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax number must contain only digits");
    }

    @Test
    @DisplayName("Should update info")
    void shouldUpdateInfo() {
        // Given
        Company company = new Company("Old Name", "1234567890");

        // When
        company.updateInfo(
                "New Name", "Tax Office", "Address", "City",
                "District", "12345", "5551112233",
                "info@new.com", "www.new.com", "USD", "INV");

        // Then
        assertThat(company.getName()).isEqualTo("New Name");
        assertThat(company.getTaxOffice()).isEqualTo("Tax Office");
        assertThat(company.getDefaultCurrency()).isEqualTo("USD");
        assertThat(company.getInvoicePrefix()).isEqualTo("INV");
    }
}
