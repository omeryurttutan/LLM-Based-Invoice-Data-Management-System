package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.application.invoice.InvoiceService;
import com.faturaocr.application.invoice.dto.CreateInvoiceCommand;
import com.faturaocr.application.invoice.dto.DuplicateCheckRequest;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.InvoiceDetailResponse;
import com.faturaocr.application.invoice.dto.InvoiceListResponse;
import com.faturaocr.application.invoice.dto.InvoiceResponse;
import com.faturaocr.application.invoice.dto.UpdateInvoiceCommand;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.infrastructure.security.annotations.CanDeleteInvoice;
import com.faturaocr.infrastructure.security.annotations.CanEditInvoice;
import com.faturaocr.interfaces.rest.invoice.dto.CreateInvoiceRequest;
import com.faturaocr.interfaces.rest.invoice.dto.DuplicateCheckRequestDTO;
import com.faturaocr.interfaces.rest.invoice.dto.DuplicateCheckResponse;
import com.faturaocr.interfaces.rest.invoice.dto.RejectInvoiceRequest;
import com.faturaocr.interfaces.rest.invoice.dto.UpdateInvoiceRequest;
import com.faturaocr.interfaces.rest.invoice.dto.VerifyInvoiceRequest;
import com.faturaocr.application.invoice.dto.RejectInvoiceCommand;
import com.faturaocr.application.invoice.dto.VerifyInvoiceCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Create a new invoice")
    @ApiResponse(responseCode = "201", description = "Invoice created successfully")
    @ApiResponse(responseCode = "409", description = "Duplicate invoice detected", content = @Content(schema = @Schema(hidden = true)))
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean forceDuplicate) {
        CreateInvoiceCommand command = mapToCommand(request);
        InvoiceResponse response = invoiceService.createInvoice(command, forceDuplicate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Check for duplicate invoices")
    @PostMapping("/check-duplicate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicate(@RequestBody DuplicateCheckRequestDTO request) {
        DuplicateCheckRequest checkRequest = DuplicateCheckRequest.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .totalAmount(request.getTotalAmount())
                .supplierName(request.getSupplierName())
                .supplierTaxNumber(request.getSupplierTaxNumber())
                .companyId(CompanyContextHolder.getCompanyId())
                .build();

        DuplicateCheckResult result = invoiceService.checkForDuplicates(checkRequest);
        return ResponseEntity.ok(DuplicateCheckResponse.success(result));
    }

    @PutMapping("/{id}")
    @CanEditInvoice
    @Operation(summary = "Update an existing invoice")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        UpdateInvoiceCommand command = mapToUpdateCommandFromUpdateRequest(request);
        return ResponseEntity.ok(invoiceService.updateInvoice(id, command));
    }

    @DeleteMapping("/{id}")
    @CanDeleteInvoice
    @Operation(summary = "Delete an invoice (soft delete)")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Verify an invoice")
    public ResponseEntity<InvoiceResponse> verifyInvoice(
            @PathVariable UUID id,
            @RequestBody(required = false) VerifyInvoiceRequest request) {
        VerifyInvoiceCommand command = new VerifyInvoiceCommand(request != null ? request.getNotes() : null);
        return ResponseEntity.ok(invoiceService.verifyInvoice(id, command));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Reject an invoice")
    public ResponseEntity<InvoiceResponse> rejectInvoice(
            @PathVariable UUID id,
            @RequestBody @Valid RejectInvoiceRequest request) {
        RejectInvoiceCommand command = new RejectInvoiceCommand(request.getRejectionReason());
        return ResponseEntity.ok(invoiceService.rejectInvoice(id, command));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Reopen a rejected invoice")
    public ResponseEntity<InvoiceResponse> reopenInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.reopenInvoice(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "List invoices with pagination")
    public ResponseEntity<Page<InvoiceListResponse>> listInvoices(
            @PageableDefault(size = 20, sort = "invoiceDate") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoices(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "List invoices by status")
    public ResponseEntity<Page<InvoiceListResponse>> listInvoicesByStatus(
            @PathVariable InvoiceStatus status,
            @PageableDefault(size = 20, sort = "invoiceDate") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoicesByStatus(status, pageable));
    }

    private CreateInvoiceCommand mapToCommand(CreateInvoiceRequest request) {
        return CreateInvoiceCommand.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .supplierName(request.getSupplierName())
                .supplierTaxNumber(request.getSupplierTaxNumber())
                .supplierTaxOffice(request.getSupplierTaxOffice())
                .supplierAddress(request.getSupplierAddress())
                .supplierPhone(request.getSupplierPhone())
                .supplierEmail(request.getSupplierEmail())
                .currency(request.getCurrency())
                .exchangeRate(request.getExchangeRate())
                .categoryId(request.getCategoryId())
                .notes(request.getNotes())
                .items(request.getItems().stream()
                        .map(item -> CreateInvoiceCommand.CreateInvoiceItemCommand.builder()
                                .description(item.getDescription())
                                .quantity(item.getQuantity())
                                .unit(item.getUnit())
                                .unitPrice(item.getUnitPrice())
                                .taxRate(item.getTaxRate())
                                .productCode(item.getProductCode())
                                .barcode(item.getBarcode())
                                .build())
                        .toList())
                .build();
    }

    private UpdateInvoiceCommand mapToUpdateCommandFromUpdateRequest(UpdateInvoiceRequest request) {
        return UpdateInvoiceCommand.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .supplierName(request.getSupplierName())
                .supplierTaxNumber(request.getSupplierTaxNumber())
                .supplierTaxOffice(request.getSupplierTaxOffice())
                .supplierAddress(request.getSupplierAddress())
                .supplierPhone(request.getSupplierPhone())
                .supplierEmail(request.getSupplierEmail())
                .currency(request.getCurrency())
                .exchangeRate(request.getExchangeRate())
                .categoryId(request.getCategoryId())
                .notes(request.getNotes())
                .items(request.getItems() != null ? request.getItems().stream()
                        .map(item -> UpdateInvoiceCommand.UpdateInvoiceItemCommand.builder()
                                .id(item.getId())
                                .description(item.getDescription())
                                .quantity(item.getQuantity())
                                .unit(item.getUnit())
                                .unitPrice(item.getUnitPrice())
                                .taxRate(item.getTaxRate())
                                .productCode(item.getProductCode())
                                .barcode(item.getBarcode())
                                .build())
                        .toList() : null)
                .build();
    }
}
