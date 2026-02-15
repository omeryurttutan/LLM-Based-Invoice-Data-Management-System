package com.faturaocr.interfaces.rest.audit;

import com.faturaocr.application.audit.AuditLogQueryService;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogFilterDTO;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for audit log queries.
 * Accessible only by ADMIN and MANAGER roles (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService queryService;

    /**
     * List audit logs with optional filtering and pagination.
     * MANAGER sees only their company's logs. ADMIN sees all.
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
            AuditLogFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> result = queryService.listAuditLogs(filter, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Get change history for a specific entity.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLogResponse>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> result = queryService.getEntityHistory(entityType, entityId, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Get activity log for a specific user (ADMIN only).
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getUserActivity(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> result = queryService.getUserActivity(userId, pageable);
        return ResponseEntity.ok(result);
    }
}
