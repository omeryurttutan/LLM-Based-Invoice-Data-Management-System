package com.faturaocr.interfaces.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.user.dto.UpdateProfileCommand;
import com.faturaocr.application.user.dto.UserProfileResponse;
import com.faturaocr.application.user.ProfileService;
import com.faturaocr.interfaces.rest.user.dto.UpdateProfileRequest;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProfileControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ProfileService profileService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private UserDetailsService userDetailsService;

        private UUID userId;
        private AuthenticatedUser principal;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                principal = new AuthenticatedUser(userId, "test@example.com", UUID.randomUUID(), "ADMIN");

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, "password",
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        public void testUpdateProfile_Success() throws Exception {
                UpdateProfileRequest request = new UpdateProfileRequest();
                request.setFullName("Updated Name");
                request.setPhone("1234567890");

                UserProfileResponse response = UserProfileResponse.builder()
                                .id(userId)
                                .fullName("Updated Name")
                                .build();

                Mockito.when(profileService.updateProfile(any(UUID.class), any(UpdateProfileCommand.class)))
                                .thenReturn(response);

                mockMvc.perform(put("/api/v1/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk());
        }
}
