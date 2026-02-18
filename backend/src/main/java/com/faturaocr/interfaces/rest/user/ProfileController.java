package com.faturaocr.interfaces.rest.user;

import com.faturaocr.application.user.ProfileService;
import com.faturaocr.application.user.dto.UserProfileResponse;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.user.dto.ChangePasswordRequest;
import com.faturaocr.interfaces.rest.user.dto.UpdateProfileRequest;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management endpoints")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile details retrieved")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        UserProfileResponse response = profileService.getProfile(currentUser.userId());
        return ApiResponse.success(response);
    }

    @PutMapping
    @Operation(summary = "Update current user profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = profileService.updateProfile(currentUser.userId(), request.toCommand());
        return ApiResponse.success("Profile updated successfully", response);
    }

    @PatchMapping("/password")
    @Operation(summary = "Change password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(currentUser.userId(), request.toCommand());
        return ApiResponse.success("Password changed successfully");
    }
}
