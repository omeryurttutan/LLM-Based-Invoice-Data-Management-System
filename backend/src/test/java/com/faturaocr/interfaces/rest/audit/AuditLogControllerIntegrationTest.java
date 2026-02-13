package com.faturaocr.interfaces.rest.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.audit.AuditLogQueryService;
import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID COMPANY_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Mock authenticated user as ADMIN
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                USER_ID, "admin@test.com", COMPANY_ID, "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null,
                Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAuditLogs_ShouldReturnOk_WhenAuthorized() throws Exception {
        AuditLogResponse response = AuditLogResponse.builder()
                .id(UUID.randomUUID())
                .actionType("CREATE")
                .entityType("INVOICE")
                .description("Test Log")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLogResponse> page = new PageImpl<>(List.of(response));

        when(queryService.listAuditLogs(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionType").value("CREATE"));
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void listAuditLogs_ShouldReturnForbidden_WhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEntityHistory_ShouldReturnOk() throws Exception {
        UUID entityId = UUID.randomUUID();
        AuditLogResponse response = AuditLogResponse.builder()
                .id(UUID.randomUUID())
                .actionType("UPDATE")
                .entityType("INVOICE")
                .entityId(entityId)
                .build();

        Page<AuditLogResponse> page = new PageImpl<>(List.of(response));

        when(queryService.getEntityHistory(eq("INVOICE"), eq(entityId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs/entity/INVOICE/" + entityId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityId").value(entityId.toString()));
    }
}
