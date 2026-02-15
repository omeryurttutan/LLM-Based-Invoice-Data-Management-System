package com.faturaocr.application.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.infrastructure.persistence.category.CategoryJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceMapper;
import com.faturaocr.infrastructure.persistence.user.UserJpaRepository;
import com.faturaocr.application.export.dto.ExportFormatMetadata;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

        private final List<InvoiceExporter> exporters;
        private final InvoiceJpaRepository invoiceRepository;
        private final InvoiceMapper invoiceMapper;
        private final UserJpaRepository userRepository;
        private final CategoryJpaRepository categoryRepository;
        private final AuditLogRepository auditLogRepository;
        private final ObjectMapper objectMapper;

        private static final int MAX_EXPORT_SIZE = 50000;

        @Transactional
        public void exportInvoices(ExportFormat format, Specification<InvoiceJpaEntity> spec, boolean includeItems,
                        OutputStream outputStream) throws IOException {

                // Accounting formats require VERIFIED status
                Specification<InvoiceJpaEntity> finalSpec = spec;
                if (isAccountingFormat(format)) {
                        finalSpec = spec.and((root, query, cb) -> cb.equal(root.get("status"),
                                        com.faturaocr.domain.invoice.valueobject.InvoiceStatus.VERIFIED));
                        includeItems = true; // Accounting formats always need items
                }

                // Count total records first
                long count = invoiceRepository.count(finalSpec);
                if (count > MAX_EXPORT_SIZE) {
                        throw new IllegalArgumentException(
                                        "Dışa aktarım limiti aşıldı. Lütfen filtrelerinizi daraltın. (Maksimum: "
                                                        + MAX_EXPORT_SIZE + " fatura)");
                }

                InvoiceExporter exporter = getExporter(format);

                // Log start
                String username = "system";
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                        if (principal instanceof com.faturaocr.infrastructure.security.AuthenticatedUser) {
                                username = ((com.faturaocr.infrastructure.security.AuthenticatedUser) principal)
                                                .email();
                        } else {
                                username = SecurityContextHolder.getContext().getAuthentication().getName();
                        }
                }
                log.info("Starting export: format={}, count={}, includeItems={}, user={}", format, count, includeItems,
                                username);

                // Create a layout iterable/stream.
                // We must use final/effectively final variable in lambda
                final Specification<InvoiceJpaEntity> effectiveSpec = finalSpec != null ? finalSpec
                                : Specification.where(null);
                boolean finalIncludeItems = includeItems;
                Iterable<List<InvoiceExportData>> dataProvider = () -> new InvoicePageIterator(
                                invoiceRepository,
                                invoiceMapper,
                                effectiveSpec,
                                finalIncludeItems,
                                userRepository,
                                categoryRepository);

                exporter.export(dataProvider, outputStream);

                // Audit Log
                auditExportAction(format, count, includeItems, username);
        }

        private InvoiceExporter getExporter(ExportFormat format) {
                return exporters.stream()
                                .filter(e -> e.getFormat() == format)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Unsupported format: " + format));
        }

        private boolean isAccountingFormat(ExportFormat format) {
                return format == ExportFormat.LOGO ||
                                format == ExportFormat.MIKRO ||
                                format == ExportFormat.NETSIS ||
                                format == ExportFormat.LUCA;
        }

        private void auditExportAction(ExportFormat format, long count, boolean includeItems, String username) {
                Map<String, Object> metadataMap = Map.of(
                                "format", format.name(),
                                "count", count,
                                "includeItems", includeItems);

                String metadataJson;
                try {
                        metadataJson = objectMapper.writeValueAsString(metadataMap);
                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize audit metadata", e);
                        metadataJson = "{}";
                }

                UUID companyId = CompanyContextHolder.getCompanyId();

                AuditLog auditLog = AuditLog.builder()
                                .actionType(AuditActionType.EXPORT)
                                .entityType("INVOICE")
                                .description("Exported " + count + " invoices")
                                .metadata(metadataJson)
                                .companyId(companyId)
                                .createdAt(LocalDateTime.now())
                                .userEmail(username)
                                .build();

                auditLogRepository.save(auditLog);
        }

        public List<ExportFormatMetadata> getExportFormats() {
                return List.of(
                                ExportFormatMetadata.builder()
                                                .format("XLSX")
                                                .label("Excel (XLSX)")
                                                .description("Microsoft Excel formatında dışa aktarım")
                                                .category("GENERAL")
                                                .fileExtension("xlsx")
                                                .icon("file-spreadsheet")
                                                .build(),
                                ExportFormatMetadata.builder()
                                                .format("CSV")
                                                .label("CSV")
                                                .description("Virgülle ayrılmış değerler formatı")
                                                .category("GENERAL")
                                                .fileExtension("csv")
                                                .icon("file-text")
                                                .build(),
                                ExportFormatMetadata.builder()
                                                .format("LOGO")
                                                .label("Logo")
                                                .description("Logo Tiger/Go/Mind muhasebe yazılımı formatı (XML)")
                                                .category("ACCOUNTING")
                                                .fileExtension("xml")
                                                .icon("building")
                                                .build(),
                                ExportFormatMetadata.builder()
                                                .format("MIKRO")
                                                .label("Mikro")
                                                .description("Mikro muhasebe yazılımı formatı (TXT)")
                                                .category("ACCOUNTING")
                                                .fileExtension("txt")
                                                .icon("building")
                                                .build(),
                                ExportFormatMetadata.builder()
                                                .format("NETSIS")
                                                .label("Netsis")
                                                .description("Netsis ERP muhasebe yazılımı formatı (XML)")
                                                .category("ACCOUNTING")
                                                .fileExtension("xml")
                                                .icon("building")
                                                .build(),
                                ExportFormatMetadata.builder()
                                                .format("LUCA")
                                                .label("Luca")
                                                .description("Luca bulut muhasebe yazılımı formatı (XLSX)")
                                                .category("ACCOUNTING")
                                                .fileExtension("xlsx")
                                                .icon("cloud")
                                                .build());
        }
}
