package com.faturaocr.interfaces.rest.rule;

import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.service.AutomationRuleService;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "Automation Rules", description = "Endpoints for managing invoice automation rules")
public class AutomationRuleController {

    private final AutomationRuleService service;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new automation rule")
    @ApiResponse(responseCode = "200", description = "Rule created successfully")
    public ResponseEntity<AutomationRule> createRule(@RequestBody AutomationRule rule) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        UUID userId = ((com.faturaocr.infrastructure.security.AuthenticatedUser) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).userId();
        rule.setCompanyId(companyId);
        rule.setCreatedByUserId(userId);
        AutomationRule created = service.createRule(rule);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<AutomationRule> updateRule(@PathVariable Long id, @RequestBody AutomationRule rule) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        AutomationRule updated = service.updateRule(id, companyId, rule);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "List automation rules")
    @ApiResponse(responseCode = "200", description = "List of rules retrieved")
    public ResponseEntity<Page<AutomationRule>> listRules(
            @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        Page<AutomationRule> page = service.listRules(companyId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get automation rule details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule found"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<AutomationRule> getRule(@PathVariable Long id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        AutomationRule rule = service.getRule(id, companyId);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rule deleted"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        service.deleteRule(id, companyId);
        return ResponseEntity.noContent().build();
    }
}
