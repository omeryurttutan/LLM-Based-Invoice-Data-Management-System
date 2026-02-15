package com.faturaocr.domain.invoice.repository.specification;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceSpecification;
import lombok.Builder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
public class InvoiceSpecificationBuilder {
    private final UUID companyId;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final String status;
    private final BigDecimal amountMin;
    private final BigDecimal amountMax;
    private final String supplierName;
    private final UUID categoryId;
    private final String currency;
    private final String sourceType;
    private final String llmProvider;
    private final BigDecimal confidenceMin;
    private final BigDecimal confidenceMax;
    private final String search;
    private final UUID createdByUserId;
    private final LocalDateTime createdFrom;
    private final LocalDateTime createdTo;

    public Specification<InvoiceJpaEntity> build() {
        List<Specification<InvoiceJpaEntity>> specs = new ArrayList<>();

        if (companyId != null) {
            specs.add(InvoiceSpecification.hasCompanyId(companyId));
        }

        if (dateFrom != null || dateTo != null) {
            specs.add(InvoiceSpecification.hasDateRange(dateFrom, dateTo));
        }

        if (StringUtils.hasText(status)) {
            // Assuming status is single value in filter panel for now, or comma separated?
            // Phase 23 implementation context implies could be list.
            // The Controller receives String. Let's assume comma separated if multiple, or
            // single.
            String[] split = status.split(",");
            List<InvoiceStatus> statuses = new ArrayList<>();
            for (String s : split) {
                if (StringUtils.hasText(s)) {
                    try {
                        statuses.add(InvoiceStatus.valueOf(s.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            if (!statuses.isEmpty()) {
                specs.add(InvoiceSpecification.hasStatuses(statuses));
            }
        }

        if (StringUtils.hasText(supplierName)) {
            specs.add(InvoiceSpecification.hasSupplierNames(List.of(supplierName)));
        }

        if (categoryId != null) {
            specs.add(InvoiceSpecification.hasCategoryIds(List.of(categoryId)));
        }

        if (amountMin != null || amountMax != null) {
            specs.add(InvoiceSpecification.hasAmountRange(amountMin, amountMax));
        }

        if (StringUtils.hasText(currency)) {
            try {
                specs.add(InvoiceSpecification.hasCurrencies(List.of(Currency.valueOf(currency.toUpperCase()))));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (StringUtils.hasText(sourceType)) {
            try {
                specs.add(InvoiceSpecification.hasSourceTypes(List.of(SourceType.valueOf(sourceType.toUpperCase()))));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (StringUtils.hasText(llmProvider)) {
            specs.add(InvoiceSpecification.hasLlmProviders(List.of(llmProvider)));
        }

        if (confidenceMin != null || confidenceMax != null) {
            specs.add(InvoiceSpecification.hasConfidenceRange(
                    confidenceMin != null ? confidenceMin.doubleValue() : null,
                    confidenceMax != null ? confidenceMax.doubleValue() : null));
        }

        if (StringUtils.hasText(search)) {
            specs.add(InvoiceSpecification.searchText(search));
        }

        if (createdByUserId != null) {
            specs.add(InvoiceSpecification.hasCreatedByUser(createdByUserId));
        }

        if (createdFrom != null || createdTo != null) {
            specs.add(InvoiceSpecification.hasCreatedDateRange(createdFrom, createdTo));
        }

        if (!Boolean.TRUE.equals(Boolean.FALSE)) { // Always add not deleted
            specs.add(InvoiceSpecification.isNotDeleted());
        }

        return specs.stream().reduce(Specification::and).orElse(null);
    }
}
