package com.faturaocr.web.controller;

import com.faturaocr.application.invoice.service.InvoiceVersionService;
import com.faturaocr.domain.invoice.dto.InvoiceVersionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceVersionController {

    private final InvoiceVersionService versionService;

    @GetMapping("/{id}/versions")
    // @PreAuthorize("@securityUtils.isInvoiceOwner(#id, authentication)")
    public ResponseEntity<List<InvoiceVersionDto.Summary>> getVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(versionService.getVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    // @PreAuthorize("@securityUtils.isInvoiceOwner(#id, authentication)")
    public ResponseEntity<InvoiceVersionDto.Detail> getVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(versionService.getVersion(id, versionNumber));
    }

    private final com.faturaocr.application.invoice.InvoiceService invoiceService;

    @GetMapping("/{id}/versions/diff")
    // @PreAuthorize("@securityUtils.isInvoiceOwner(#id, authentication)")
    public ResponseEntity<InvoiceVersionDto.VersionDiff> getDiff(
            @PathVariable UUID id,
            @RequestParam Integer from,
            @RequestParam Integer to) {
        return ResponseEntity.ok(versionService.compareVersions(id, from, to));
    }

    @PostMapping("/{id}/revert/{versionNumber}")
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<com.faturaocr.application.invoice.dto.InvoiceResponse> revertInvoice(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(invoiceService.revertInvoice(id, versionNumber));
    }
}
