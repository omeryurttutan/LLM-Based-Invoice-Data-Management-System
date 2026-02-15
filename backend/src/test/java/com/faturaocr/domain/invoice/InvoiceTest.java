package com.faturaocr.domain.invoice;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InvoiceTest {

    @Test
    @DisplayName("Should create invoice with default values")
    void shouldCreateInvoiceWithDefaultValues() {
        // When
        Invoice invoice = new Invoice();

        // Then
        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getCurrency()).isEqualTo(Currency.TRY);
        assertThat(invoice.getSubtotal()).isEqualTo(BigDecimal.ZERO);
        assertThat(invoice.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(invoice.getItems()).isEmpty();
        assertThat(invoice.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should calculate totals correctly when items are added")
    void shouldCalculateTotalsCorrectlyWhenItemsAreAdded() {
        // Given
        Invoice invoice = new Invoice();

        InvoiceItem item1 = TestDataBuilder.anInvoiceItem()
                .withQuantity(new BigDecimal("2"))
                .withUnitPrice(new BigDecimal("100.00")) // 200.00 subtotal
                .build(); // Tax 18% -> 36.00 -> Total 236.00

        InvoiceItem item2 = TestDataBuilder.anInvoiceItem()
                .withQuantity(new BigDecimal("1"))
                .withUnitPrice(new BigDecimal("50.00")) // 50.00 subtotal
                .build(); // Tax 18% -> 9.00 -> Total 59.00

        // When
        invoice.addItem(item1);
        invoice.addItem(item2);
        invoice.calculateTotals();

        // Then
        assertThat(invoice.getSubtotal()).isEqualTo(new BigDecimal("250.00"));
        assertThat(invoice.getTaxAmount()).isEqualTo(new BigDecimal("45.00"));
        assertThat(invoice.getTotalAmount()).isEqualTo(new BigDecimal("295.00"));
    }

    @Test
    @DisplayName("Should remove item and recalculate totals")
    void shouldRemoveItemAndRecalculateTotals() {
        // Given
        Invoice invoice = new Invoice();
        InvoiceItem item1 = TestDataBuilder.anInvoiceItem().withUnitPrice(new BigDecimal("100.00")).build();
        InvoiceItem item2 = TestDataBuilder.anInvoiceItem().withUnitPrice(new BigDecimal("200.00")).build();

        invoice.addItem(item1);
        invoice.addItem(item2);
        invoice.calculateTotals();

        BigDecimal initialTotal = invoice.getTotalAmount();

        // When
        invoice.removeItem(item1);
        invoice.calculateTotals();

        // Then
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getTotalAmount()).isLessThan(initialTotal);
        assertThat(invoice.getTotalAmount()).isEqualTo(item2.getTotalAmount());
    }

    @Test
    @DisplayName("Should transition status correctly")
    void shouldTransitionStatusCorrectly() {
        // Given
        Invoice invoice = new Invoice();
        UUID userId = UUID.randomUUID();

        // 1. PENDING -> VERIFIED
        invoice.verify(userId);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VERIFIED);
        assertThat(invoice.getVerifiedAt()).isNotNull();
        assertThat(invoice.getVerifiedByUserId()).isEqualTo(userId);

        // 2. VERIFIED -> PENDING (Reopen)
        invoice.reopen();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getVerifiedAt()).isNull();
        assertThat(invoice.getVerifiedByUserId()).isNull();

        // 3. PENDING -> REJECTED
        invoice.reject("Invalid data");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
        assertThat(invoice.getRejectedAt()).isNotNull();
        assertThat(invoice.getRejectionReason()).isEqualTo("Invalid data");

        // 4. REJECTED -> PENDING
        invoice.reopen();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getRejectedAt()).isNull();
        assertThat(invoice.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("Should throw exception on invalid status transitions")
    void shouldThrowExceptionOnInvalidStatusTransitions() {
        Invoice invoice = new Invoice();
        UUID userId = UUID.randomUUID();

        // PENDING -> VERIFIED
        invoice.verify(userId);

        // VERIFIED -> VERIFIED (Invalid)
        Throwable thrown = catchThrowable(() -> invoice.verify(userId));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Invoice is already verified");

        // VERIFIED -> REJECTED (Invalid)
        thrown = catchThrowable(() -> invoice.reject("Reason"));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot reject a verified invoice. Reopen it first.");

        // Reset to PENDING -> REJECTED
        invoice.reopen();
        invoice.reject("Bad");

        // REJECTED -> REJECTED (Invalid)
        thrown = catchThrowable(() -> invoice.reject("Reason"));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Invoice is already rejected");

        // REJECTED -> VERIFIED (Invalid)
        thrown = catchThrowable(() -> invoice.verify(userId));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot verify a rejected invoice. Reopen it first.");
    }
}
