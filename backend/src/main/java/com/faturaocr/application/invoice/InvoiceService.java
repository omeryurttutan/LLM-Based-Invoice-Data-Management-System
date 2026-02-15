package com.faturaocr.application.invoice;

import com.faturaocr.application.common.exception.BusinessException;
import com.faturaocr.application.common.exception.DuplicateInvoiceException;
import com.faturaocr.application.common.exception.ResourceNotFoundException;
import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.application.invoice.dto.CreateInvoiceCommand;
import com.faturaocr.application.invoice.dto.DuplicateCheckRequest;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.FilterOptionsResponse;
import com.faturaocr.application.invoice.dto.InvoiceDetailResponse;

import com.faturaocr.application.invoice.dto.InvoiceListResponse;
import com.faturaocr.application.invoice.dto.InvoiceResponse;
import com.faturaocr.application.invoice.dto.RejectInvoiceCommand;
import com.faturaocr.application.invoice.dto.UpdateInvoiceCommand;
import com.faturaocr.application.invoice.dto.VerifyInvoiceCommand;
import com.faturaocr.application.invoice.service.InvoiceVersionService;
import com.faturaocr.domain.audit.annotation.Auditable;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;
import com.faturaocr.domain.template.service.SupplierTemplateService;
import com.faturaocr.domain.rule.service.RuleEngine;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import com.faturaocr.infrastructure.audit.AuditRequestContext;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceSpecification;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.interfaces.rest.invoice.dto.InvoiceFilterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceJpaRepository invoiceJpaRepository;
    private final CategoryRepository categoryRepository;

    private final DuplicateDetectionService duplicateDetectionService;
    private final InvoiceDTOMapper mapper;
    private final com.faturaocr.domain.notification.service.NotificationService notificationService;
    private final InvoiceVersionService versionService;
    private final SupplierTemplateService supplierTemplateService;
    private final RuleEngine ruleEngine;

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
            AuditRequestContext.setMetadata(
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

        // Run automation rules for manual creation
        ruleEngine.evaluateAndExecute(TriggerPoint.ON_MANUAL_CREATE, invoice);

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

    public Page<InvoiceListResponse> listInvoices(InvoiceFilterRequest filter, Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();

        Specification<InvoiceJpaEntity> spec = Specification.where(InvoiceSpecification.hasCompanyId(companyId))
                .and(InvoiceSpecification.isNotDeleted());

        if (filter != null) {
            spec = spec.and(InvoiceSpecification.hasDateRange(filter.getDateFrom(), filter.getDateTo()))
                    .and(InvoiceSpecification.hasStatuses(filter.getStatus()))
                    .and(InvoiceSpecification.hasSupplierNames(filter.getSupplierName()))
                    .and(InvoiceSpecification.hasCategoryIds(filter.getCategoryId()))
                    .and(InvoiceSpecification.hasAmountRange(filter.getAmountMin(), filter.getAmountMax()))
                    .and(InvoiceSpecification.hasCurrencies(filter.getCurrency()))
                    .and(InvoiceSpecification.hasSourceTypes(filter.getSourceType()))
                    .and(InvoiceSpecification.hasLlmProviders(filter.getLlmProvider()))
                    .and(InvoiceSpecification.hasConfidenceRange(filter.getConfidenceMin(), filter.getConfidenceMax()))
                    .and(InvoiceSpecification.searchText(filter.getSearch()))
                    .and(InvoiceSpecification.hasCreatedByUser(filter.getCreatedByUserId()))
                    .and(InvoiceSpecification.hasCreatedDateRange(filter.getCreatedFrom(), filter.getCreatedTo()));
        }
        @SuppressWarnings("null")
        Page<InvoiceJpaEntity> page = invoiceJpaRepository.findAll(spec, pageable);
        return page.map(mapper::mapJpaToListResponse);
    }

    public Page<InvoiceListResponse> listInvoices(Pageable pageable) {
        return listInvoices(null, pageable);
    }

    public List<String> getSuppliers(String search) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        // Limit to 50 results
        return invoiceJpaRepository.findDistinctSupplierNames(companyId, search, Pageable.ofSize(50));
    }

    public FilterOptionsResponse getFilterOptions() {
        UUID companyId = CompanyContextHolder.getCompanyId();

        // Get dynamic data from DB
        List<LlmProvider> distinctProviders = invoiceJpaRepository.findDistinctLlmProviders(companyId);
        Object[] amountRange = invoiceJpaRepository.findMinMaxTotalAmount(companyId);
        Object[] dateRange = invoiceJpaRepository.findMinMaxInvoiceDate(companyId);
        Object[] confidenceRange = invoiceJpaRepository.findMinMaxConfidenceScore(companyId);

        // Process ranges safely
        BigDecimal minAmount = amountRange != null && amountRange[0] != null ? (BigDecimal) amountRange[0]
                : BigDecimal.ZERO;
        BigDecimal maxAmount = amountRange != null && amountRange[1] != null ? (BigDecimal) amountRange[1]
                : BigDecimal.ZERO;

        LocalDate minDate = dateRange != null && dateRange[0] != null ? (LocalDate) dateRange[0] : null;
        LocalDate maxDate = dateRange != null && dateRange[1] != null ? (LocalDate) dateRange[1] : null;

        Double minConfidence = confidenceRange != null && confidenceRange[0] != null
                ? ((BigDecimal) confidenceRange[0]).doubleValue()
                : null;
        Double maxConfidence = confidenceRange != null && confidenceRange[1] != null
                ? ((BigDecimal) confidenceRange[1]).doubleValue()
                : null;

        // Build Response
        return FilterOptionsResponse.builder()
                .statuses(Arrays.stream(InvoiceStatus.values())
                        .map(s -> FilterOptionsResponse.StatusOption.builder().value(s).label(s.name()).build())
                        .collect(Collectors.toList()))
                .categories(categoryRepository.findAllByCompanyId(companyId).stream()
                        .map(c -> FilterOptionsResponse.CategoryOption.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .color(c.getColor())
                                .build())
                        .collect(Collectors.toList()))
                .currencies(Arrays.asList(Currency.values()))
                .sourceTypes(Arrays.asList(SourceType.values()))
                .llmProviders(distinctProviders)
                .amountRange(FilterOptionsResponse.Range.<BigDecimal>builder().min(minAmount).max(maxAmount).build())
                .dateRange(FilterOptionsResponse.Range.<LocalDate>builder().min(minDate).max(maxDate).build())
                .confidenceRange(
                        FilterOptionsResponse.Range.<Double>builder().min(minConfidence).max(maxConfidence).build())
                .build();
    }

    public InvoiceDetailResponse getInvoiceById(UUID id) {
        Invoice invoice = getInvoiceOrThrow(id);
        return mapper.mapToDetailResponse(invoice);
    }

    @Auditable(action = AuditActionType.UPDATE, entityType = "INVOICE")
    public InvoiceResponse updateInvoice(UUID id, UpdateInvoiceCommand command) {
        Invoice invoice = getInvoiceOrThrow(id);
        UUID companyId = CompanyContextHolder.getCompanyId();

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("INVOICE_NOT_EDITABLE",
                    "Cannot edit invoice with status: " + invoice.getStatus());
        }

        // Create version snapshot before update
        versionService.createSnapshot(invoice, invoice.getItems(),
                com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource.MANUAL_EDIT,
                "Manual invoice update");

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

        // Update extraction corrections if provided
        if (command.getExtractionCorrections() != null) {
            invoice.setExtractionCorrections(command.getExtractionCorrections());
        }

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

        // Learn from verified invoice
        supplierTemplateService.learnFromInvoice(invoice);

        // Run automation rules after verification
        ruleEngine.evaluateAndExecute(TriggerPoint.AFTER_VERIFICATION, invoice);

        // Save again if rules modified something?
        // ruleEngine modifies the entity object. But strict JPA might need save?
        // If evaluateAndExecute is transactional and modifies the attached entity, it
        // might flush automatically.
        // But InvoiceService method is also transactional (by class or method?).
        // @ApplicationService custom annotation likely has @Transactional?
        // Let's assume explicit save is safer if rules modify it.
        // Actually, if ruleEngine works on 'invoice' object and we are in transaction,
        // dirty checking works.
        // But let's add specific save if rules modified it?
        // Or just let the transaction commit handle it.
        // However, we just called save(invoice) above.
        // Best to call rule execution BEFORE save, or save AGAIN.
        // Let's call save again to be sure if changes happened.
        invoiceRepository.save(invoice);

        // Notify verification
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("invoiceNumber", invoice.getInvoiceNumber());

        notificationService.notify(
                invoice.getCreatedByUserId(),
                invoice.getCompanyId(),
                com.faturaocr.domain.notification.enums.NotificationType.INVOICE_VERIFIED,
                "Fatura Doğrulandı",
                String.format("%s numaralı fatura doğrulandı.", invoice.getInvoiceNumber()),
                NotificationSeverity.SUCCESS,
                NotificationReferenceType.INVOICE,
                invoice.getId(),
                metadata);

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

        // Notify rejection
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("invoiceNumber", invoice.getInvoiceNumber());
        metadata.put("reason", invoice.getRejectionReason());

        notificationService.notify(
                invoice.getCreatedByUserId(),
                invoice.getCompanyId(),
                com.faturaocr.domain.notification.enums.NotificationType.INVOICE_REJECTED,
                "Fatura Reddedildi",
                String.format("%s numaralı fatura reddedildi. Sebep: %s",
                        invoice.getInvoiceNumber(), invoice.getRejectionReason()),
                NotificationSeverity.WARNING,
                NotificationReferenceType.INVOICE,
                invoice.getId(),
                metadata);

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

    @Auditable(action = AuditActionType.UPDATE, entityType = "INVOICE")
    public InvoiceResponse revertInvoice(UUID id, Integer versionNumber) {
        Invoice currentInvoice = getInvoiceOrThrow(id);

        // snapshot current state before revert
        versionService.createSnapshot(currentInvoice, currentInvoice.getItems(),
                com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource.REVERT,
                "Reverting to version " + versionNumber);

        // Get reverted data
        Invoice revertedData = versionService.revertToVersion(id, versionNumber);

        // Apply changes to current entity
        // We copy fields from revertedData to currentInvoice
        // ID remains same. CompanyID remains same.
        // User/Dates might need handling?
        // CreatedAt should be original? Yes.
        // UpdatedAt will change automatically.
        // Status? Reverted to snapshot status.

        currentInvoice.setInvoiceNumber(revertedData.getInvoiceNumber());
        currentInvoice.setInvoiceDate(revertedData.getInvoiceDate());
        currentInvoice.setDueDate(revertedData.getDueDate());
        currentInvoice.setSupplierName(revertedData.getSupplierName());
        currentInvoice.setSupplierTaxNumber(revertedData.getSupplierTaxNumber());
        currentInvoice.setSupplierTaxOffice(revertedData.getSupplierTaxOffice());
        currentInvoice.setSupplierAddress(revertedData.getSupplierAddress());
        currentInvoice.setSupplierPhone(revertedData.getSupplierPhone());
        currentInvoice.setSupplierEmail(revertedData.getSupplierEmail());
        currentInvoice.setSubtotal(revertedData.getSubtotal());
        currentInvoice.setTaxAmount(revertedData.getTaxAmount());
        currentInvoice.setTotalAmount(revertedData.getTotalAmount());
        currentInvoice.setCurrency(revertedData.getCurrency());
        currentInvoice.setExchangeRate(revertedData.getExchangeRate());
        currentInvoice.setStatus(revertedData.getStatus());
        currentInvoice.setSourceType(revertedData.getSourceType());
        currentInvoice.setLlmProvider(revertedData.getLlmProvider());
        currentInvoice.setConfidenceScore(revertedData.getConfidenceScore());
        currentInvoice.setNotes(revertedData.getNotes());
        currentInvoice.setCategoryId(revertedData.getCategoryId());

        // Handle items
        // Clear existing items?
        // Invoice entity has `items` list.
        // We can't just set list because of JPA management (orphan removal etc).
        // Best to clear and add.

        // Create a temporary list of new items
        List<InvoiceItem> newItems = new ArrayList<>();
        if (revertedData.getItems() != null) {
            for (InvoiceItem itemData : revertedData.getItems()) {
                InvoiceItem newItem = new InvoiceItem();
                // Copy item fields
                newItem.setDescription(itemData.getDescription());
                newItem.setQuantity(itemData.getQuantity());
                newItem.setUnit(itemData.getUnit());
                newItem.setUnitPrice(itemData.getUnitPrice());
                newItem.setTaxRate(itemData.getTaxRate());
                newItem.setSubtotal(itemData.getSubtotal());
                newItem.setTaxAmount(itemData.getTaxAmount());
                newItem.setTotalAmount(itemData.getTotalAmount());
                newItem.setProductCode(itemData.getProductCode());
                newItem.setBarcode(itemData.getBarcode());
                newItem.setLineNumber(itemData.getLineNumber());
                newItems.add(newItem);
            }
        }

        // Remove all current items
        currentInvoice.getItems().clear();

        // Add all new items
        for (InvoiceItem item : newItems) {
            currentInvoice.addItem(item);
        }

        invoiceRepository.save(currentInvoice);

        return InvoiceResponse.builder()
                .id(id)
                .message("Reverted to version " + versionNumber)
                .build();
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

}
