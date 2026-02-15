package com.faturaocr.infrastructure.persistence.invoice;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    public Invoice toDomain(InvoiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Invoice invoice = new Invoice();
        invoice.setId(entity.getId());
        invoice.setCompanyId(entity.getCompanyId());
        invoice.setCategoryId(entity.getCategoryId());
        invoice.setCreatedByUserId(entity.getCreatedByUserId());
        invoice.setVerifiedByUserId(entity.getVerifiedByUserId());
        invoice.setBatchId(entity.getBatchId());
        invoice.setInvoiceNumber(entity.getInvoiceNumber());
        invoice.setInvoiceDate(entity.getInvoiceDate());
        invoice.setDueDate(entity.getDueDate());
        invoice.setSupplierName(entity.getSupplierName());
        invoice.setSupplierTaxNumber(entity.getSupplierTaxNumber());
        invoice.setSupplierTaxOffice(entity.getSupplierTaxOffice());
        invoice.setSupplierAddress(entity.getSupplierAddress());
        invoice.setSupplierPhone(entity.getSupplierPhone());
        invoice.setSupplierEmail(entity.getSupplierEmail());
        invoice.setSubtotal(entity.getSubtotal());
        invoice.setTaxAmount(entity.getTaxAmount());
        invoice.setTotalAmount(entity.getTotalAmount());
        invoice.setCurrency(entity.getCurrency());
        invoice.setExchangeRate(entity.getExchangeRate());
        invoice.setStatus(entity.getStatus());
        invoice.setSourceType(entity.getSourceType());
        invoice.setLlmProvider(entity.getLlmProvider());
        invoice.setConfidenceScore(entity.getConfidenceScore());
        invoice.setProcessingDurationMs(entity.getProcessingDurationMs());
        invoice.setOriginalFilePath(entity.getOriginalFilePath());
        invoice.setStoredFilePath(entity.getStoredFilePath());
        invoice.setFileHash(entity.getFileHash());
        invoice.setOriginalFileName(entity.getOriginalFileName());
        invoice.setOriginalFileSize(entity.getOriginalFileSize());
        invoice.setOriginalFileType(entity.getOriginalFileType());
        invoice.setEInvoiceUuid(entity.getEInvoiceUuid());
        invoice.setEInvoiceEttn(entity.getEInvoiceEttn());
        invoice.setNotes(entity.getNotes());
        invoice.setRejectionReason(entity.getRejectionReason());
        invoice.setVerifiedAt(entity.getVerifiedAt());
        invoice.setRejectedAt(entity.getRejectedAt());
        invoice.setDeleted(entity.isDeleted());
        invoice.setDeletedAt(entity.getDeletedAt());
        invoice.setCreatedAt(entity.getCreatedAt());
        invoice.setUpdatedAt(entity.getUpdatedAt());
        invoice.setExtractionCorrections(entity.getExtractionCorrections());

        if (entity.getItems() != null) {
            List<InvoiceItem> items = entity.getItems().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
            invoice.setItems(items);
        }

        return invoice;
    }

    public InvoiceItem toDomain(InvoiceItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        InvoiceItem item = new InvoiceItem();
        item.setId(entity.getId());
        if (entity.getInvoice() != null) {
            item.setInvoiceId(entity.getInvoice().getId());
        }
        item.setLineNumber(entity.getLineNumber());
        item.setDescription(entity.getDescription());
        item.setQuantity(entity.getQuantity());
        item.setUnit(entity.getUnit());
        item.setUnitPrice(entity.getUnitPrice());
        item.setTaxRate(entity.getTaxRate());
        item.setTaxAmount(entity.getTaxAmount());
        item.setSubtotal(entity.getSubtotal());
        item.setTotalAmount(entity.getTotalAmount());
        item.setProductCode(entity.getProductCode());
        item.setBarcode(entity.getBarcode());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    public InvoiceJpaEntity toJpa(Invoice domain) {
        if (domain == null) {
            return null;
        }

        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setCreatedByUserId(domain.getCreatedByUserId());
        entity.setVerifiedByUserId(domain.getVerifiedByUserId());
        entity.setBatchId(domain.getBatchId());
        entity.setInvoiceNumber(domain.getInvoiceNumber());
        entity.setInvoiceDate(domain.getInvoiceDate());
        entity.setDueDate(domain.getDueDate());
        entity.setSupplierName(domain.getSupplierName());
        entity.setSupplierTaxNumber(domain.getSupplierTaxNumber());
        entity.setSupplierTaxOffice(domain.getSupplierTaxOffice());
        entity.setSupplierAddress(domain.getSupplierAddress());
        entity.setSupplierPhone(domain.getSupplierPhone());
        entity.setSupplierEmail(domain.getSupplierEmail());
        entity.setSubtotal(domain.getSubtotal());
        entity.setTaxAmount(domain.getTaxAmount());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setExchangeRate(domain.getExchangeRate());
        entity.setStatus(domain.getStatus());
        entity.setSourceType(domain.getSourceType());
        entity.setLlmProvider(domain.getLlmProvider());
        entity.setConfidenceScore(domain.getConfidenceScore());
        entity.setProcessingDurationMs(domain.getProcessingDurationMs());
        entity.setOriginalFilePath(domain.getOriginalFilePath());
        entity.setStoredFilePath(domain.getStoredFilePath());
        entity.setFileHash(domain.getFileHash());
        entity.setOriginalFileName(domain.getOriginalFileName());
        entity.setOriginalFileSize(domain.getOriginalFileSize());
        entity.setOriginalFileType(domain.getOriginalFileType());
        entity.setEInvoiceUuid(domain.getEInvoiceUuid());
        entity.setEInvoiceEttn(domain.getEInvoiceEttn());
        entity.setNotes(domain.getNotes());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setVerifiedAt(domain.getVerifiedAt());
        entity.setRejectedAt(domain.getRejectedAt());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setExtractionCorrections(domain.getExtractionCorrections());
        if (domain.getItems() != null) {
            for (InvoiceItem item : domain.getItems()) {
                InvoiceItemJpaEntity itemEntity = toJpa(item);
                entity.addItem(itemEntity);
            }
        }

        return entity;
    }

    public InvoiceItemJpaEntity toJpa(InvoiceItem domain) {
        if (domain == null) {
            return null;
        }

        InvoiceItemJpaEntity entity = new InvoiceItemJpaEntity();
        entity.setId(domain.getId());
        // invoice relation is handled by parent add method
        entity.setLineNumber(domain.getLineNumber());
        entity.setDescription(domain.getDescription());
        entity.setQuantity(domain.getQuantity());
        entity.setUnit(domain.getUnit());
        entity.setUnitPrice(domain.getUnitPrice());
        entity.setTaxRate(domain.getTaxRate());
        entity.setTaxAmount(domain.getTaxAmount());
        entity.setSubtotal(domain.getSubtotal());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setProductCode(domain.getProductCode());
        entity.setBarcode(domain.getBarcode());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
