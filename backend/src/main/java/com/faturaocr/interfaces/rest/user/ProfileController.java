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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        UserProfileResponse response = profileService.getProfile(currentUser.userId());
        return ApiResponse.success(response);
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = profileService.updateProfile(currentUser.userId(), request.toCommand());
        return ApiResponse.success("Profile updated successfully", response);
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(currentUser.userId(), request.toCommand());
        return ApiResponse.success("Password changed successfully");
    }
}
