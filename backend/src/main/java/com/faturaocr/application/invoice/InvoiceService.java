package com.faturaocr.application.invoice;

import com.faturaocr.application.common.exception.BusinessException;
import com.faturaocr.application.common.exception.DuplicateInvoiceException;
import com.faturaocr.application.common.exception.ResourceNotFoundException;
import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.application.invoice.dto.*;

import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.domain.audit.annotation.Auditable;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CategoryRepository categoryRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    @Auditable(action = AuditActionType.CREATE, entityType = "INVOICE")
    public InvoiceResponse createInvoice(CreateInvoiceCommand command, boolean forceDuplicate) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        UUID userId = getCurrentUserId();

        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException("INVOICE_ITEMS_REQUIRED", "At least one item required");
        }

        // Run duplicate detection (unless force flag is set)
        if (!forceDuplicate) {
            DuplicateCheckResult dupResult = checkForDuplicatesInternal(
                    command.getInvoiceNumber(), command.getInvoiceDate(),
                    command.getSupplierName(), command.getSupplierTaxNumber(),
                    command, companyId, null);

            // Block on HIGH and MEDIUM matches; LOW is informational only
            if (dupResult.isHasDuplicates() &&
                    (dupResult.getHighestConfidence() == DuplicateConfidence.HIGH ||
                            dupResult.getHighestConfidence() == DuplicateConfidence.MEDIUM)) {
                throw new DuplicateInvoiceException(dupResult);
            }
        } else {
            // Log that this was a forced duplicate creation
            com.faturaocr.infrastructure.audit.AuditRequestContext.setMetadata(
                    "{\"forcedDuplicate\": true, \"highestConfidence\": \"HIGH\"}");
        }

        Invoice invoice = new Invoice();
        invoice.setCompanyId(companyId);
        invoice.setCreatedByUserId(userId);
        invoice.setInvoiceNumber(command.getInvoiceNumber());
        invoice.setInvoiceDate(command.getInvoiceDate());
        invoice.setDueDate(command.getDueDate());
        invoice.setSupplierName(command.getSupplierName());
        invoice.setSupplierTaxNumber(command.getSupplierTaxNumber());
        invoice.setSupplierTaxOffice(command.getSupplierTaxOffice());
        invoice.setSupplierAddress(command.getSupplierAddress());
        invoice.setSupplierPhone(command.getSupplierPhone());
        invoice.setSupplierEmail(command.getSupplierEmail());
        invoice.setCurrency(Currency.valueOf(command.getCurrency()));
        invoice.setExchangeRate(command.getExchangeRate() != null ? command.getExchangeRate() : BigDecimal.ONE);
        invoice.setNotes(command.getNotes());
        invoice.setSourceType(SourceType.MANUAL);
        invoice.setStatus(InvoiceStatus.PENDING);

        if (command.getCategoryId() != null) {
            validateCategory(command.getCategoryId(), companyId);
            invoice.setCategoryId(command.getCategoryId());
        }

        int lineNumber = 1;
        for (CreateInvoiceCommand.CreateInvoiceItemCommand itemCmd : command.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setLineNumber(lineNumber++);
            item.setDescription(itemCmd.getDescription());
            item.setQuantity(itemCmd.getQuantity());
            item.setUnit(itemCmd.getUnit());
            item.setUnitPrice(itemCmd.getUnitPrice());
            item.setTaxRate(itemCmd.getTaxRate());
            item.setProductCode(itemCmd.getProductCode());
            item.setBarcode(itemCmd.getBarcode());

            // Calculate item totals
            calculateItemTotals(item);
            invoice.addItem(item);
        }

        invoice.calculateTotals();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceResponse.builder()
                .id(savedInvoice.getId())
                .message("Invoice created successfully")
                .build();
    }

    /**
     * Public method for standalone duplicate check (used by check-duplicate
     * endpoint).
     */
    public DuplicateCheckResult checkForDuplicates(DuplicateCheckRequest request) {
        return duplicateDetectionService.checkForDuplicates(request);
    }

    public Page<InvoiceListResponse> listInvoices(Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        return invoiceRepository.findAllByCompanyId(companyId, pageable)
                .map(this::mapToListResponse);
    }

    public Page<InvoiceListResponse> listInvoicesByStatus(InvoiceStatus status, Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        return invoiceRepository.findAllByCompanyIdAndStatus(companyId, status, pageable)
                .map(this::mapToListResponse);
    }

    public InvoiceDetailResponse getInvoiceById(UUID id) {
        Invoice invoice = getInvoiceOrThrow(id);
        return mapToDetailResponse(invoice);
    }

    @Auditable(action = AuditActionType.UPDATE, entityType = "INVOICE")
    public InvoiceResponse updateInvoice(UUID id, UpdateInvoiceCommand command) {
        Invoice invoice = getInvoiceOrThrow(id);
        UUID companyId = CompanyContextHolder.getCompanyId();

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_NOT_EDITABLE",
                    "Cannot edit invoice with status: " + invoice.getStatus());
        }

        // Run duplicate check if invoice number changed, excluding self
        if (!invoice.getInvoiceNumber().equals(command.getInvoiceNumber())) {
            DuplicateCheckResult dupResult = checkForDuplicatesInternal(
                    command.getInvoiceNumber(), command.getInvoiceDate(),
                    command.getSupplierName(), command.getSupplierTaxNumber(),
                    null, companyId, id);

            if (dupResult.isHasDuplicates() &&
                    (dupResult.getHighestConfidence() == DuplicateConfidence.HIGH ||
                            dupResult.getHighestConfidence() == DuplicateConfidence.MEDIUM)) {
                throw new DuplicateInvoiceException(dupResult);
            }
        }

        invoice.setInvoiceNumber(command.getInvoiceNumber());
        invoice.setInvoiceDate(command.getInvoiceDate());
        invoice.setDueDate(command.getDueDate());
        invoice.setSupplierName(command.getSupplierName());
        invoice.setSupplierTaxNumber(command.getSupplierTaxNumber());
        invoice.setSupplierTaxOffice(command.getSupplierTaxOffice());
        invoice.setSupplierAddress(command.getSupplierAddress());
        invoice.setSupplierPhone(command.getSupplierPhone());
        invoice.setSupplierEmail(command.getSupplierEmail());
        invoice.setCurrency(Currency.valueOf(command.getCurrency()));
        invoice.setExchangeRate(command.getExchangeRate());
        invoice.setNotes(command.getNotes());

        if (command.getCategoryId() != null) {
            validateCategory(command.getCategoryId(), companyId);
            invoice.setCategoryId(command.getCategoryId());
        } else {
            invoice.setCategoryId(null);
        }

        // Update items
        if (command.getItems() != null) {
            updateItems(invoice, command.getItems());
        }

        invoice.calculateTotals();
        invoiceRepository.save(invoice);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .message("Invoice updated successfully")
                .build();
    }

    @Auditable(action = AuditActionType.DELETE, entityType = "INVOICE")
    public void deleteInvoice(UUID id) {
        Invoice invoice = getInvoiceOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.VERIFIED) {
            throw new BusinessException("INVOICE_NOT_DELETABLE", "Cannot delete VERIFIED invoice");
        }
        invoiceRepository.softDelete(id);
    }

    @Auditable(action = AuditActionType.VERIFY, entityType = "INVOICE")
    public InvoiceResponse verifyInvoice(UUID id, VerifyInvoiceCommand command) {
        Invoice invoice = getInvoiceOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_INVALID_STATUS_TRANSITION", "Invoice is not PENDING");
        }

        invoice.setStatus(InvoiceStatus.VERIFIED);
        invoice.setVerifiedByUserId(getCurrentUserId());
        invoice.setVerifiedAt(LocalDateTime.now());
        if (command != null && StringUtils.hasText(command.getNotes())) {
            invoice.setNotes(invoice.getNotes() + "\n[Verify Note]: " + command.getNotes());
        }

        invoiceRepository.save(invoice);
        return InvoiceResponse.builder().id(id).message("Invoice verified").build();
    }

    @Auditable(action = AuditActionType.REJECT, entityType = "INVOICE")
    public InvoiceResponse rejectInvoice(UUID id, RejectInvoiceCommand command) {
        Invoice invoice = getInvoiceOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_INVALID_STATUS_TRANSITION", "Invoice is not PENDING");
        }

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setVerifiedByUserId(getCurrentUserId()); // Rejected by
        invoice.setRejectedAt(LocalDateTime.now());
        if (command != null) {
            invoice.setRejectionReason(command.getRejectionReason());
        }

        invoiceRepository.save(invoice);
        return InvoiceResponse.builder().id(id).message("Invoice rejected").build();
    }

    public InvoiceResponse reopenInvoice(UUID id) {
        Invoice invoice = getInvoiceOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.REJECTED) {
            throw new BusinessException("INVOICE_INVALID_STATUS_TRANSITION", "Only REJECTED invoice can be reopened");
        }

        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setVerifiedByUserId(null); // Clear verifier/rejecter
        invoice.setVerifiedAt(null);
        invoice.setRejectedAt(null);
        invoice.setRejectionReason(null);

        invoiceRepository.save(invoice);
        return InvoiceResponse.builder().id(id).message("Invoice reopened").build();
    }

    private Invoice getInvoiceOrThrow(UUID id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        return invoiceRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) auth.getPrincipal()).userId();
        }
        // Fallback or throw if not authenticated (should be handled by security filter)
        throw new BusinessException("UNAUTHORIZED", "User not authenticated");
    }

    private void validateCategory(UUID categoryId, UUID companyId) {
        if (!categoryRepository.findByIdAndCompanyId(categoryId, companyId).isPresent()) {
            throw new ResourceNotFoundException("Category not found");
        }
    }

    /**
     * Internal helper to build a DuplicateCheckRequest and run the detection.
     * Calculates totalAmount from command items when available.
     */
    private DuplicateCheckResult checkForDuplicatesInternal(
            String invoiceNumber, java.time.LocalDate invoiceDate,
            String supplierName, String supplierTaxNumber,
            CreateInvoiceCommand command, UUID companyId, UUID excludeInvoiceId) {

        BigDecimal totalAmount = null;
        if (command != null && command.getItems() != null) {
            totalAmount = BigDecimal.ZERO;
            for (CreateInvoiceCommand.CreateInvoiceItemCommand itemCmd : command.getItems()) {
                BigDecimal qty = itemCmd.getQuantity();
                BigDecimal price = itemCmd.getUnitPrice();
                BigDecimal subtotal = qty.multiply(price);
                BigDecimal tax = subtotal.multiply(itemCmd.getTaxRate()).divide(new BigDecimal(100));
                totalAmount = totalAmount.add(subtotal).add(tax);
            }
        }

        DuplicateCheckRequest dupRequest = DuplicateCheckRequest.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate)
                .totalAmount(totalAmount)
                .supplierName(supplierName)
                .supplierTaxNumber(supplierTaxNumber)
                .companyId(companyId)
                .excludeInvoiceId(excludeInvoiceId)
                .build();

        return duplicateDetectionService.checkForDuplicates(dupRequest);
    }

    private void calculateItemTotals(InvoiceItem item) {
        BigDecimal quantity = item.getQuantity();
        BigDecimal unitPrice = item.getUnitPrice();
        BigDecimal subtotal = quantity.multiply(unitPrice);
        BigDecimal taxRate = item.getTaxRate();
        BigDecimal taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal(100));
        BigDecimal totalAmount = subtotal.add(taxAmount);

        item.setSubtotal(subtotal);
        item.setTaxAmount(taxAmount);
        item.setTotalAmount(totalAmount);
    }

    private void updateItems(Invoice invoice, List<UpdateInvoiceCommand.UpdateInvoiceItemCommand> itemCommands) {
        Map<UUID, UpdateInvoiceCommand.UpdateInvoiceItemCommand> commandMap = itemCommands.stream()
                .filter(cmd -> cmd.getId() != null)
                .collect(Collectors.toMap(UpdateInvoiceCommand.UpdateInvoiceItemCommand::getId, Function.identity()));

        // Remove items not in command (orphan removal via domain logic)
        List<InvoiceItem> itemsToRemove = new ArrayList<>();
        for (InvoiceItem existingItem : invoice.getItems()) {
            if (!commandMap.containsKey(existingItem.getId())) {
                itemsToRemove.add(existingItem);
            }
        }
        itemsToRemove.forEach(invoice::removeItem);

        // Update existing items
        for (InvoiceItem existingItem : invoice.getItems()) {
            UpdateInvoiceCommand.UpdateInvoiceItemCommand cmd = commandMap.get(existingItem.getId());
            if (cmd != null) {
                existingItem.setDescription(cmd.getDescription());
                existingItem.setQuantity(cmd.getQuantity());
                existingItem.setUnit(cmd.getUnit());
                existingItem.setUnitPrice(cmd.getUnitPrice());
                existingItem.setTaxRate(cmd.getTaxRate());
                existingItem.setProductCode(cmd.getProductCode());
                existingItem.setBarcode(cmd.getBarcode());
                calculateItemTotals(existingItem);
            }
        }

        // Add new items
        int nextLineNumber = invoice.getItems().stream()
                .mapToInt(InvoiceItem::getLineNumber)
                .max().orElse(0) + 1;

        for (UpdateInvoiceCommand.UpdateInvoiceItemCommand cmd : itemCommands) {
            if (cmd.getId() == null) {
                InvoiceItem newItem = new InvoiceItem();
                newItem.setLineNumber(nextLineNumber++);
                newItem.setDescription(cmd.getDescription());
                newItem.setQuantity(cmd.getQuantity());
                newItem.setUnit(cmd.getUnit());
                newItem.setUnitPrice(cmd.getUnitPrice());
                newItem.setTaxRate(cmd.getTaxRate());
                newItem.setProductCode(cmd.getProductCode());
                newItem.setBarcode(cmd.getBarcode());
                calculateItemTotals(newItem);
                invoice.addItem(newItem);
            }
        }
    }

    private InvoiceListResponse mapToListResponse(Invoice invoice) {
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
        response.setItemCount(invoice.getItems().size());
        response.setCreatedAt(invoice.getCreatedAt());
        // Populate category name and user name if needed (omitted for performance or
        // fetched via joins)
        // For now just basic mapping
        if (invoice.getCategoryId() != null) {
            categoryRepository.findById(invoice.getCategoryId()).ifPresent(c -> response.setCategoryName(c.getName()));
        }
        return response;
    }

    private InvoiceDetailResponse mapToDetailResponse(Invoice invoice) {
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
}
