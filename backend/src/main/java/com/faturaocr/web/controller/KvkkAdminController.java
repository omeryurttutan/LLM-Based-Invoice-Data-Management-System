package com.faturaocr.web.controller;

import com.faturaocr.application.kvkk.service.KvkkReportService;
import com.faturaocr.application.kvkk.service.RightToBeForgottenService;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.web.dto.kvkk.KvkkReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class KvkkAdminController {

    private final RightToBeForgottenService rightToBeForgottenService;
    private final KvkkReportService kvkkReportService;

    @PostMapping("/gdpr/forget/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forgetUser(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable UUID userId) {

        rightToBeForgottenService.forgetUser(admin.userId(), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/kvkk/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KvkkReportResponse> getComplianceReport() {
        return ResponseEntity.ok(kvkkReportService.getComplianceReport());
    }

    @GetMapping("/consent/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KvkkReportResponse> getConsentReport() {
        // Reusing the main report for now as it contains consent summary
        return ResponseEntity.ok(kvkkReportService.getComplianceReport());
    }
}
