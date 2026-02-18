package com.faturaocr.interfaces.rest.kvkk;

import com.faturaocr.application.kvkk.service.KvkkReportService;
import com.faturaocr.application.kvkk.service.RightToBeForgottenService;
import com.faturaocr.web.dto.kvkk.KvkkReportResponse;
import com.faturaocr.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/admin/kvkk")
@RequiredArgsConstructor
@Tag(name = "KVKK Admin", description = "Admin endpoints for KVKK compliance")
@PreAuthorize("hasRole('ADMIN')")
public class KvkkController {

    private final RightToBeForgottenService rightToBeForgottenService;
    private final KvkkReportService kvkkReportService;

    @PostMapping("/forget/{userId}")
    @Operation(summary = "Right to be Forgotten", description = "Anonymize user data and delete personal information. Irreversible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User data anonymized"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> forgetUser(@PathVariable UUID userId) {
        rightToBeForgottenService.forgetUser(SecurityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/report")
    @Operation(summary = "KVKK Compliance Report", description = "Get a summary report of KVKK compliance status")
    @ApiResponse(responseCode = "200", description = "Compliance report retrieved")
    public ResponseEntity<KvkkReportResponse> getComplianceReport() {
        return ResponseEntity.ok(kvkkReportService.getComplianceReport());
    }
}
