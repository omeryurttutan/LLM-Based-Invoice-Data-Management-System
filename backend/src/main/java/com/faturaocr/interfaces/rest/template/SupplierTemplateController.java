package com.faturaocr.interfaces.rest.template;

import com.faturaocr.domain.template.entity.SupplierTemplate;
import com.faturaocr.domain.template.service.SupplierTemplateService;
import com.faturaocr.interfaces.rest.template.dto.SupplierTemplateResponse;
import com.faturaocr.interfaces.rest.template.dto.UpdateDefaultCategoryRequest;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Supplier Templates", description = "Endpoints for managing supplier learning templates")
public class SupplierTemplateController {

    private final SupplierTemplateService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "List supplier templates")
    @ApiResponse(responseCode = "200", description = "List of templates retrieved")
    public ResponseEntity<Page<SupplierTemplateResponse>> listTemplates(
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        Page<SupplierTemplate> page = service.listTemplates(companyId, pageable);
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Get template details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template details retrieved"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<SupplierTemplateResponse> getTemplate(@PathVariable Long id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        SupplierTemplate template = service.getTemplate(id, companyId);
        return ResponseEntity.ok(toResponse(template));
    }

    @PutMapping("/{id}/default-category")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Set default category for a template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default category updated"),
            @ApiResponse(responseCode = "404", description = "Template or category not found")
    })
    public ResponseEntity<SupplierTemplateResponse> updateDefaultCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDefaultCategoryRequest request) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        SupplierTemplate template = service.updateDefaultCategory(id, companyId, request.getCategoryId());
        return ResponseEntity.ok(toResponse(template));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Toggle template active status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Toggle successful"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    public ResponseEntity<SupplierTemplateResponse> toggleActive(@PathVariable Long id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        SupplierTemplate template = service.toggleActive(id, companyId);
        return ResponseEntity.ok(toResponse(template));
    }

    private SupplierTemplateResponse toResponse(SupplierTemplate domain) {
        return SupplierTemplateResponse.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .supplierTaxNumber(domain.getSupplierTaxNumber())
                .supplierName(domain.getSupplierName())
                .sampleCount(domain.getSampleCount())
                .learnedData(domain.getLearnedData())
                .defaultCategoryId(domain.getDefaultCategoryId())
                .active(domain.isActive())
                .lastInvoiceDate(domain.getLastInvoiceDate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
