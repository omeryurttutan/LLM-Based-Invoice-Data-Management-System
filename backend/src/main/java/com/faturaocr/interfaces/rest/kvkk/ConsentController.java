package com.faturaocr.interfaces.rest.kvkk;

import com.faturaocr.application.kvkk.service.ConsentService;
import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import com.faturaocr.infrastructure.security.SecurityUtils;
import com.faturaocr.web.dto.kvkk.ConsentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
@Tag(name = "Consent Management", description = "Endpoints for managing user consents")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    @Operation(summary = "Record user consent", description = "Grant or revoke consent for a specific type")
    @ApiResponse(responseCode = "200", description = "Consent recorded successfully")
    public ResponseEntity<Void> recordConsent(@RequestBody ConsentRequest request, HttpServletRequest httpRequest) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID companyId = SecurityUtils.getCurrentCompanyId();

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        consentService.recordConsent(userId, companyId, request.getConsentType(), request.getConsentVersion(),
                request.isGranted(),
                ipAddress, userAgent);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get current consents", description = "Get current consent status for all types")
    @ApiResponse(responseCode = "200", description = "Current consents retrieved")
    public ResponseEntity<Map<ConsentType, Boolean>> getCurrentConsents() {
        return ResponseEntity.ok(consentService.getCurrentConsents(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get consent history", description = "Get full history of consent changes for current user")
    @ApiResponse(responseCode = "200", description = "Consent history retrieved")
    public ResponseEntity<List<UserConsent>> getConsentHistory() {
        return ResponseEntity.ok(consentService.getConsentHistory(SecurityUtils.getCurrentUserId()));
    }
}
