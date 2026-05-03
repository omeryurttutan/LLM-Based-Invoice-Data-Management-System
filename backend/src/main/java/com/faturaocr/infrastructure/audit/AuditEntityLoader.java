package com.faturaocr.infrastructure.audit;

import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Strategy-based loader that retrieves the current state of an entity
 * by type and ID, before an auditable method modifies it.
 * Used to capture the "old_value" in audit logs.
 */
@Component
@RequiredArgsConstructor
public class AuditEntityLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditEntityLoader.class);

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Load the current entity state given its type and ID.
     * Returns null if not found or type is unknown.
     */
    public Object loadEntity(String entityType, UUID entityId) {
        if (entityId == null) {
            return null;
        }
        try {
            return switch (entityType.toUpperCase()) {
                case "INVOICE" -> invoiceRepository.findById(entityId).orElse(null);
                case "COMPANY" -> companyRepository.findById(entityId).orElse(null);
                case "USER" -> userRepository.findById(entityId).orElse(null);
                case "CATEGORY" -> categoryRepository.findById(entityId).orElse(null);
                default -> {
                    LOGGER.warn("Unknown entity type for audit loading: {}", entityType);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.warn("Failed to load entity for audit (type={}, id={}): {}",
                    entityType, entityId, e.getMessage());
            return null;
        }
    }
}
