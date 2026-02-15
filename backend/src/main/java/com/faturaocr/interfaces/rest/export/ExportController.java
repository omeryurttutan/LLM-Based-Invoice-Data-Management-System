package com.faturaocr.interfaces.rest.export;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.ExportService;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceSpecification;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.interfaces.rest.invoice.dto.InvoiceFilterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Data export endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

        private final ExportService exportService;

        @GetMapping("/export")
        @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
        @Operation(summary = "Export invoices to Excel or CSV")
        public void exportInvoices(
                        InvoiceFilterRequest filter,
                        @RequestParam(defaultValue = "XLSX") ExportFormat format,
                        @RequestParam(defaultValue = "false") boolean includeItems,
                        HttpServletResponse response) throws IOException {

                // Build Specification (Reusing Phase 23-A logic)
                UUID companyId = CompanyContextHolder.getCompanyId();
                Specification<InvoiceJpaEntity> spec = Specification.where(InvoiceSpecification.hasCompanyId(companyId))
                                .and(InvoiceSpecification.isNotDeleted());

                if (filter != null) {
                        spec = spec.and(InvoiceSpecification.hasDateRange(filter.getDateFrom(), filter.getDateTo()))
                                        .and(InvoiceSpecification.hasStatuses(filter.getStatus()))
                                        .and(InvoiceSpecification.hasSupplierNames(filter.getSupplierName()))
                                        .and(InvoiceSpecification.hasCategoryIds(filter.getCategoryId()))
                                        .and(InvoiceSpecification.hasAmountRange(filter.getAmountMin(),
                                                        filter.getAmountMax()))
                                        .and(InvoiceSpecification.hasCurrencies(filter.getCurrency()))
                                        .and(InvoiceSpecification.hasSourceTypes(filter.getSourceType()))
                                        .and(InvoiceSpecification.hasLlmProviders(filter.getLlmProvider()))
                                        .and(InvoiceSpecification.hasConfidenceRange(filter.getConfidenceMin(),
                                                        filter.getConfidenceMax()))
                                        .and(InvoiceSpecification.searchText(filter.getSearch()))
                                        .and(InvoiceSpecification.hasCreatedByUser(filter.getCreatedByUserId()))
                                        .and(InvoiceSpecification.hasCreatedDateRange(filter.getCreatedFrom(),
                                                        filter.getCreatedTo()));
                }

                // Set Response Headers
                String extension;
                String contentType;

                switch (format) {
                        case XLSX:
                        case LUCA:
                                extension = "xlsx";
                                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                                break;
                        case LOGO:
                        case NETSIS:
                                extension = "xml";
                                contentType = "application/xml";
                                break;
                        case MIKRO:
                                extension = "txt";
                                contentType = "text/plain;charset=windows-1254";
                                break;
                        case CSV:
                        default:
                                extension = "csv";
                                contentType = "text/csv; charset=UTF-8";
                                break;
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
                // For accounting formats, we might want specific filenames as tested?
                // Test expects: faturalar_LOGO.xml
                String filename;
                if (format == ExportFormat.XLSX || format == ExportFormat.CSV) {
                        filename = "faturalar_" + timestamp + "." + extension;
                } else {
                        filename = "faturalar_" + format.name() + "." + extension;
                }

                response.setContentType(contentType);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

                // Execute Export
                exportService.exportInvoices(format, spec, includeItems, response.getOutputStream());
        }
}
