package com.faturaocr.interfaces.rest.auth;

import com.faturaocr.application.auth.dto.AuthResponse;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.application.auth.dto.RefreshTokenCommand;
import com.faturaocr.application.auth.dto.RegisterCommand;
import com.faturaocr.application.auth.service.AuthenticationService;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.common.BaseController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.faturaocr.infrastructure.security.SecurityUtils;
import java.util.UUID;

/**
 * Authentication REST controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController extends BaseController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a new user account and company. If companyCode is provided, joins an existing company. Returns access and refresh tokens.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterCommand command) {

        AuthResponse response = authenticationService.register(command);
        return created(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Returns information about the currently logged-in user. Requires valid Bearer token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AuthResponse.UserInfo userInfo = authenticationService.getCurrentUser(userId);
        return ok(userInfo);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain JWT tokens", description = "Authenticates a user with email and password. Returns a JWT access token and a refresh token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "Account locked due to too many failed attempts")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginCommand command) {

        AuthResponse response = authenticationService.login(command);
        return ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an expired access token", description = "Uses a valid refresh token to obtain a new access token. Does not require authentication header.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenCommand command) {

        AuthResponse response = authenticationService.refresh(command);
        return ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate session", description = "Invalidates the provided refresh token. Requires Bearer authentication.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized or invalid token")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenCommand command) {

        authenticationService.logout(command.refreshToken());
        return ok("Logged out successfully", null);
    }
}
