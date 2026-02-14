package com.faturaocr.interfaces.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.user.ProfileService;
import com.faturaocr.application.user.dto.ChangePasswordCommand;
import com.faturaocr.application.user.dto.UpdateProfileCommand;
import com.faturaocr.application.user.dto.UserProfileResponse;
import com.faturaocr.interfaces.rest.user.dto.ChangePasswordRequest;
import com.faturaocr.interfaces.rest.user.dto.UpdateProfileRequest;
import com.faturaocr.infrastructure.security.test.WithMockAuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @Test
    @WithMockAuthenticatedUser
    void getMyProfile_ReturnsProfile() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .fullName("My Profile")
                .build();

        when(profileService.getProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("My Profile"));
    }

    @Test
    @WithMockAuthenticatedUser
    void updateProfile_ReturnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        UserProfileResponse response = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Updated Name")
                .build();

        when(profileService.updateProfile(any(), any(UpdateProfileCommand.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    @WithMockAuthenticatedUser
    void changePassword_ReturnsSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("NewPass1!");

        mockMvc.perform(patch("/api/v1/profile/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(profileService).changePassword(any(), any(ChangePasswordCommand.class));
    }
}
