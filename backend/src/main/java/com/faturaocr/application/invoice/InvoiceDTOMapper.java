package com.faturaocr.application.invoice;

import com.faturaocr.application.invoice.dto.InvoiceDetailResponse;
import com.faturaocr.application.invoice.dto.InvoiceItemResponse;
import com.faturaocr.application.invoice.dto.InvoiceListResponse;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvoiceDTOMapper {

    private final CategoryRepository categoryRepository;

    public InvoiceDetailResponse mapToDetailResponse(Invoice invoice) {
        InvoiceDetailResponse response = new InvoiceDetailResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDueDate(invoice.getDueDate());
        response.setSupplierName(invoice.getSupplierName());
        response.setSupplierTaxNumber(invoice.getSupplierTaxNumber());
        response.setSupplierTaxOffice(invoice.getSupplierTaxOffice());
        response.setSupplierAddress(invoice.getSupplierAddress());
        response.setSupplierPhone(invoice.getSupplierPhone());
        response.setSupplierEmail(invoice.getSupplierEmail());
        response.setSubtotal(invoice.getSubtotal());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setCurrency(invoice.getCurrency());
        response.setExchangeRate(invoice.getExchangeRate());
        response.setStatus(invoice.getStatus());
        response.setSourceType(invoice.getSourceType());
        response.setLlmProvider(invoice.getLlmProvider());
        response.setConfidenceScore(invoice.getConfidenceScore());
        response.setCategoryId(invoice.getCategoryId());
        response.setNotes(invoice.getNotes());
        response.setRejectionReason(invoice.getRejectionReason());
        response.setCreatedByUserId(invoice.getCreatedByUserId());
        response.setVerifiedByUserId(invoice.getVerifiedByUserId());
        response.setVerifiedAt(invoice.getVerifiedAt());
        response.setRejectedAt(invoice.getRejectedAt());
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());

        if (invoice.getCategoryId() != null) {
            categoryRepository.findById(invoice.getCategoryId()).ifPresent(c -> response.setCategoryName(c.getName()));
        }

        List<InvoiceItemResponse> itemResponses = invoice.getItems().stream().map(item -> {
            InvoiceItemResponse itemResp = new InvoiceItemResponse();
            itemResp.setId(item.getId());
            itemResp.setLineNumber(item.getLineNumber());
            itemResp.setDescription(item.getDescription());
            itemResp.setQuantity(item.getQuantity());
            itemResp.setUnit(item.getUnit());
            itemResp.setUnitPrice(item.getUnitPrice());
            itemResp.setTaxRate(item.getTaxRate());
            itemResp.setTaxAmount(item.getTaxAmount());
            itemResp.setSubtotal(item.getSubtotal());
            itemResp.setTotalAmount(item.getTotalAmount());
            itemResp.setProductCode(item.getProductCode());
            itemResp.setBarcode(item.getBarcode());
            return itemResp;
        }).collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    public InvoiceListResponse mapJpaToListResponse(InvoiceJpaEntity invoice) {
        InvoiceListResponse response = new InvoiceListResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDueDate(invoice.getDueDate());
        response.setSupplierName(invoice.getSupplierName());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setCurrency(invoice.getCurrency());
        response.setStatus(invoice.getStatus());
        response.setSourceType(invoice.getSourceType());
        if (invoice.getItems() != null) {
            response.setItemCount(invoice.getItems().size());
        } else {
            response.setItemCount(0);
        }
        response.setCreatedAt(invoice.getCreatedAt());

        if (invoice.getCategoryId() != null) {
            categoryRepository.findById(invoice.getCategoryId()).ifPresent(c -> response.setCategoryName(c.getName()));
        }
        return response;
    }

    public InvoiceListResponse mapJpaToListResponse(InvoiceJpaEntity invoice,
            java.util.Map<java.util.UUID, String> categoryNameMap) {
        InvoiceListResponse response = new InvoiceListResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDueDate(invoice.getDueDate());
        response.setSupplierName(invoice.getSupplierName());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setCurrency(invoice.getCurrency());
        response.setStatus(invoice.getStatus());
        response.setSourceType(invoice.getSourceType());
        if (invoice.getItems() != null) {
            response.setItemCount(invoice.getItems().size());
        } else {
            response.setItemCount(0);
        }
        response.setCreatedAt(invoice.getCreatedAt());

        if (invoice.getCategoryId() != null && categoryNameMap != null) {
            response.setCategoryName(categoryNameMap.get(invoice.getCategoryId()));
        }
        return response;
    }
}
