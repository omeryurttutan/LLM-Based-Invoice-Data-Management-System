package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.application.invoice.InvoiceService;
import com.faturaocr.application.invoice.dto.CreateInvoiceCommand;
import com.faturaocr.application.invoice.dto.DuplicateCheckRequest;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.InvoiceDetailResponse;
import com.faturaocr.application.invoice.dto.InvoiceListResponse;
import com.faturaocr.application.invoice.dto.InvoiceResponse;
import com.faturaocr.application.invoice.dto.UpdateInvoiceCommand;
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
import com.faturaocr.application.invoice.dto.FilterOptionsResponse;
import com.faturaocr.application.invoice.dto.InvoiceFilterRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final com.faturaocr.application.export.ExportService exportService;

    @Operation(summary = "Create a new invoice", description = "Creates a new invoice manually. Validates input and checks for duplicates. Returns the created invoice details.")
    @ApiResponse(responseCode = "201", description = "Invoice created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid validation error")
    @ApiResponse(responseCode = "409", description = "Duplicate invoice detected (unless forceDuplicate=true)", content = @Content(schema = @Schema(implementation = DuplicateCheckResponse.class)))
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean forceDuplicate) {
        CreateInvoiceCommand command = mapToCommand(request);
        InvoiceResponse response = invoiceService.createInvoice(command, forceDuplicate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Check for duplicate invoices", description = "Checks if an invoice with similar details already exists to prevent duplication.")
    @ApiResponse(responseCode = "200", description = "Check completed successfully")
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
    @Operation(summary = "Update an existing invoice", description = "Updates invoice details. Only allowed for editable invoices (PENDING, REJECTED).")
    @ApiResponse(responseCode = "200", description = "Invoice updated successfully")
    @ApiResponse(responseCode = "403", description = "Invoice cannot be edited in current status")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        UpdateInvoiceCommand command = mapToUpdateCommandFromUpdateRequest(request);
        return ResponseEntity.ok(invoiceService.updateInvoice(id, command));
    }

    @DeleteMapping("/{id}")
    @CanDeleteInvoice
    @Operation(summary = "Delete an invoice", description = "Soft deletes an invoice. Only allowed if user has permission and invoice is in deletable status.")
    @ApiResponse(responseCode = "204", description = "Invoice deleted successfully")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions or invoice cannot be deleted")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Verify an invoice", description = "Changes invoice status to VERIFIED or PROCESSING based on workflow.")
    @ApiResponse(responseCode = "200", description = "Invoice verified successfully")
    @ApiResponse(responseCode = "422", description = "Invalid status transition")
    public ResponseEntity<InvoiceResponse> verifyInvoice(
            @PathVariable UUID id,
            @RequestBody(required = false) VerifyInvoiceRequest request) {
        VerifyInvoiceCommand command = new VerifyInvoiceCommand(request != null ? request.getNotes() : null);
        return ResponseEntity.ok(invoiceService.verifyInvoice(id, command));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Reject an invoice", description = "Changes invoice status to REJECTED. Requires a rejection reason.")
    @ApiResponse(responseCode = "200", description = "Invoice rejected successfully")
    public ResponseEntity<InvoiceResponse> rejectInvoice(
            @PathVariable UUID id,
            @RequestBody @Valid RejectInvoiceRequest request) {
        RejectInvoiceCommand command = new RejectInvoiceCommand(request.getRejectionReason());
        return ResponseEntity.ok(invoiceService.rejectInvoice(id, command));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Reopen a rejected invoice", description = "Moves a REJECTED invoice back to PENDING status.")
    @ApiResponse(responseCode = "200", description = "Invoice reopened successfully")
    public ResponseEntity<InvoiceResponse> reopenInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.reopenInvoice(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Get invoice details", description = "Retrieves full invoice details including line items.")
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "List invoices", description = "List invoices with pagination, sorting, and advanced filtering options.")
    @ApiResponse(responseCode = "200", description = "List of invoices retrieved")
    public ResponseEntity<Page<InvoiceListResponse>> listInvoices(
            @Valid InvoiceFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "invoiceDate") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoices(filterRequest, pageable));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Autocomplete supplier names", description = "Returns unique supplier names matching the search term.")
    public ResponseEntity<List<String>> getSuppliers(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(invoiceService.getSuppliers(search));
    }

    @GetMapping("/filter-options")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Get filter options", description = "Returns available filter ranges (min/max amounts, dates) and distinct values.")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        return ResponseEntity.ok(invoiceService.getFilterOptions());
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
                .extractionCorrections(request.getExtractionCorrections())
                .build();
    }

    @GetMapping("/export/formats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get export formats", description = "Returns available export formats for invoices.")
    public ResponseEntity<List<com.faturaocr.application.export.dto.ExportFormatMetadata>> getExportFormats() {
        return ResponseEntity.ok(exportService.getExportFormats());
    }
}
