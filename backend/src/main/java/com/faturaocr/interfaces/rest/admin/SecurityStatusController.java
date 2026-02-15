package com.faturaocr.interfaces.rest.admin;

import com.faturaocr.infrastructure.security.LoginAttemptService;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.common.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/security")
@Tag(name = "Security Status", description = "Security configuration status endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityStatusController extends BaseController {

    private final LoginAttemptService loginAttemptService;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.security.rate-limit.public-limit:20}")
    private int publicLimit;

    @Value("${app.security.rate-limit.login-limit:5}")
    private int loginLimit;

    @Value("${app.security.rate-limit.authenticated-limit:100}")
    private int authenticatedLimit;

    @Value("${app.security.rate-limit.upload-limit:10}")
    private int uploadLimit;

    @Value("${app.security.rate-limit.export-limit:5}")
    private int exportLimit;

    @Value("${app.security.rate-limit.admin-limit:50}")
    private int adminLimit;

    @Value("${app.security.headers.hsts-enabled:false}")
    private boolean hstsEnabled;

    @Value("${app.security.headers.csp-enabled:true}")
    private boolean cspEnabled;

    @Value("${app.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${server.tomcat.max-http-form-post-size:20MB}")
    private String maxPostSize;

    @Value("${app.security.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.login.lockout-duration-minutes:15}")
    private int lockoutDuration;

    @Value("${app.kvkk.encryption.enabled:false}")
    private boolean encryptionEnabled;

    public SecurityStatusController(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get current security configuration status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();

        // Rate Limiting
        Map<String, Object> rateLimit = new HashMap<>();
        rateLimit.put("enabled", rateLimitEnabled);
        Map<String, Integer> tiers = new HashMap<>();
        tiers.put("PUBLIC", publicLimit);
        tiers.put("LOGIN", loginLimit);
        tiers.put("AUTHENTICATED", authenticatedLimit);
        tiers.put("UPLOAD", uploadLimit);
        tiers.put("EXPORT", exportLimit);
        tiers.put("ADMIN", adminLimit);
        rateLimit.put("tiers", tiers);
        status.put("rateLimiting", rateLimit);

        // Brute Force
        Map<String, Object> bruteForce = new HashMap<>();
        bruteForce.put("enabled", true);
        bruteForce.put("maxAttempts", maxLoginAttempts);
        bruteForce.put("lockoutDurationMinutes", lockoutDuration);
        status.put("bruteForceProtection", bruteForce);

        // Headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("hstsEnabled", hstsEnabled);
        headers.put("cspEnabled", cspEnabled);
        status.put("headers", headers);

        // CORS
        Map<String, Object> cors = new HashMap<>();
        cors.put("allowedOrigins", allowedOrigins);
        status.put("cors", cors);

        // Request Limits
        Map<String, Object> requestLimits = new HashMap<>();
        requestLimits.put("maxPostSize", maxPostSize);
        status.put("requestLimits", requestLimits);

        // Encryption
        status.put("encryptionEnabled", encryptionEnabled);

        return ok(status);
    }
}
