package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceSpecificationTest {

    @Test
    void hasCompanyId_ShouldReturnSpecification() {
        UUID companyId = UUID.randomUUID();
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasCompanyId(companyId);
        assertThat(spec).isNotNull();
    }

    @Test
    void hasStatuses_ShouldReturnNull_WhenListIsEmpty() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasStatuses(Collections.emptyList());
        assertThat(spec).isNull();
    }

    @Test
    void hasStatuses_ShouldReturnSpecification_WhenListIsNotEmpty() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasStatuses(List.of(InvoiceStatus.PENDING));
        assertThat(spec).isNotNull();
    }

    @Test
    void hasDateRange_ShouldReturnNull_WhenBothNull() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasDateRange(null, null);
        assertThat(spec).isNull();
    }

    @Test
    void hasDateRange_ShouldReturnSpecification_WhenOneIsNotNull() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasDateRange(LocalDate.now(), null);
        assertThat(spec).isNotNull();
    }

    @Test
    void hasAmountRange_ShouldReturnSpecification_WhenValuesProvided() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.hasAmountRange(BigDecimal.ZERO, BigDecimal.TEN);
        assertThat(spec).isNotNull();
    }

    @Test
    void searchText_ShouldReturnNull_WhenSearchIsEmpty() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.searchText("");
        assertThat(spec).isNull();
    }

    @Test
    void searchText_ShouldReturnSpecification_WhenSearchIsNotEmpty() {
        Specification<InvoiceJpaEntity> spec = InvoiceSpecification.searchText("search");
        assertThat(spec).isNotNull();
    }
}
