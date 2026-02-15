package com.faturaocr.web.controller;

import com.faturaocr.application.kvkk.service.ConsentService;
import com.faturaocr.domain.kvkk.entity.UserConsent;
import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.web.dto.kvkk.ConsentRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    public ResponseEntity<Void> recordConsent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ConsentRequest request,
            HttpServletRequest servletRequest) {

        String ip = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        consentService.recordConsent(
                user.userId(),
                user.companyId(),
                request.getConsentType(),
                request.getConsentVersion(),
                request.isGranted(),
                ip,
                userAgent);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Map<ConsentType, Boolean>> getConsents(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(consentService.getCurrentConsents(user.userId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<UserConsent>> getHistory(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(consentService.getConsentHistory(user.userId()));
    }
}
