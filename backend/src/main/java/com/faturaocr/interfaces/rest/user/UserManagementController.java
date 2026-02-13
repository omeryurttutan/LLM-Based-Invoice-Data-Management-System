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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userManagementService.createUser(request.toCommand());
        return ApiResponse.success("User created successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Page<UserResponse>> listUsers(Pageable pageable) {
        Page<UserResponse> response = userManagementService.listUsersByCompany(pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userManagementService.getUserById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userManagementService.updateUser(id, request.toCommand());
        return ApiResponse.success("User updated successfully", response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userManagementService.deleteUser(id);
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> toggleUserActive(@PathVariable UUID id) {
        UserResponse response = userManagementService.toggleUserActive(id);
        String message = response.isActive() ? "User activated" : "User deactivated";
        return ApiResponse.success(message, response);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> changeUserRole(@PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        UserResponse response = userManagementService.changeUserRole(id, request.toCommand());
        return ApiResponse.success("User role updated successfully", response);
    }
}
