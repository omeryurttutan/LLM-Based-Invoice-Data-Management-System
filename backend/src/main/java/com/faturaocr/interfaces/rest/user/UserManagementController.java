package com.faturaocr.interfaces.rest.user;

import com.faturaocr.application.user.UserManagementService;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.user.dto.ChangeRoleRequest;
import com.faturaocr.interfaces.rest.user.dto.CreateUserRequest;
import com.faturaocr.interfaces.rest.user.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin user management endpoints")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userManagementService.createUser(request.toCommand());
        return ApiResponse.success("User created successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @Operation(summary = "List users for company")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of users retrieved")
    public ApiResponse<Page<UserResponse>> listUsers(Pageable pageable) {
        Page<UserResponse> response = userManagementService.listUsersByCompany(pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @Operation(summary = "Get user details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userManagementService.getUserById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @Operation(summary = "Update user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userManagementService.updateUser(id, request.toCommand());
        return ApiResponse.success("User updated successfully", response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted successfully")
    public void deleteUser(@PathVariable UUID id) {
        userManagementService.deleteUser(id);
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @Operation(summary = "Toggle user active status")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status toggled")
    public ApiResponse<UserResponse> toggleUserActive(@PathVariable UUID id) {
        UserResponse response = userManagementService.toggleUserActive(id);
        String message = response.isActive() ? "User activated" : "User deactivated";
        return ApiResponse.success(message, response);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    @Operation(summary = "Change user role")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User role updated")
    public ApiResponse<UserResponse> changeUserRole(@PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        UserResponse response = userManagementService.changeUserRole(id, request.toCommand());
        return ApiResponse.success("User role updated successfully", response);
    }
}
