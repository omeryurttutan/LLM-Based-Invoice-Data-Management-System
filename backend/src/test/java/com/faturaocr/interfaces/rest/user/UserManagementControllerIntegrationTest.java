package com.faturaocr.interfaces.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.user.UserManagementService;
import com.faturaocr.application.user.dto.ChangeRoleCommand;
import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UpdateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.interfaces.rest.user.dto.ChangeRoleRequest;
import com.faturaocr.interfaces.rest.user.dto.CreateUserRequest;
import com.faturaocr.interfaces.rest.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementService userManagementService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_WhenAdmin_ReturnsCreated() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@test.com");
        request.setFullName("Test User");
        request.setPassword("Password123!");
        request.setRole(Role.ACCOUNTANT);

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .fullName("Test User")
                .role(Role.ACCOUNTANT)
                .build();

        when(userManagementService.createUser(any(CreateUserCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("user@test.com"));
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void createUser_WhenAccountant_ReturnsForbidden() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@test.com");
        request.setFullName("Test User");
        request.setPassword("Password123!");
        request.setRole(Role.ACCOUNTANT);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_WhenAdmin_ReturnsList() throws Exception {
        UserResponse user = UserResponse.builder().email("user@test.com").build();
        Page<UserResponse> page = new PageImpl<>(Collections.singletonList(user));

        when(userManagementService.listUsersByCompany(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("user@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_WhenAdmin_ReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setRole(Role.MANAGER);

        UserResponse response = UserResponse.builder()
                .id(id)
                .role(Role.MANAGER)
                .build();

        when(userManagementService.changeUserRole(eq(id), any(ChangeRoleCommand.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/{id}/role", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MANAGER"));
    }
}
