package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InvoiceSpecification {

    public static Specification<InvoiceJpaEntity> hasCompanyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<InvoiceJpaEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<InvoiceJpaEntity> hasDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), dateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<InvoiceJpaEntity> hasStatuses(List<InvoiceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<InvoiceJpaEntity> hasSupplierNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            // LIKE matching with OR for each name, case-insensitive
            List<Predicate> predicates = new ArrayList<>();
            for (String name : names) {
                if (StringUtils.hasText(name)) { // Using hasText instead of isNotEmpty
                    predicates.add(cb.like(cb.lower(root.get("supplierName")), "%" + name.toLowerCase() + "%"));
                }
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<InvoiceJpaEntity> hasCategoryIds(List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("categoryId").in(categoryIds);
    }

    public static Specification<InvoiceJpaEntity> hasAmountRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), max));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<InvoiceJpaEntity> hasCurrencies(List<Currency> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("currency").in(currencies);
    }

    public static Specification<InvoiceJpaEntity> hasSourceTypes(List<SourceType> sourceTypes) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("sourceType").in(sourceTypes);
    }

    public static Specification<InvoiceJpaEntity> hasLlmProviders(List<String> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            // Convert string providers to Enum if needed, but assuming string based on
            // Entity definition or Enum mapping
            // Entity defines LlmProvider enum.
            // We need to map boolean/string to Enum
            try {
                List<LlmProvider> enumProviders = providers.stream()
                        .map(p -> LlmProvider.valueOf(p.toUpperCase()))
                        .collect(Collectors.toList());
                return root.get("llmProvider").in(enumProviders);
            } catch (IllegalArgumentException e) {
                return null; // Ignore invalid values
            }
        };
    }

    public static Specification<InvoiceJpaEntity> hasConfidenceRange(Double min, Double max) {
        if ((min == null && max == null)) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // precision 5, 2 in DB.
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("confidenceScore"), BigDecimal.valueOf(min)));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("confidenceScore"), BigDecimal.valueOf(max)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<InvoiceJpaEntity> searchText(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return (root, query, cb) -> {
            String likePattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("invoiceNumber")), likePattern),
                    cb.like(cb.lower(root.get("supplierName")), likePattern),
                    // buyer_name is not in InvoiceJpaEntity shown in view_file, skipping
                    cb.like(cb.lower(root.get("notes")), likePattern));
        };
    }

    public static Specification<InvoiceJpaEntity> hasCreatedByUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("createdByUserId"), userId);
    }

    public static Specification<InvoiceJpaEntity> hasCreatedDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
