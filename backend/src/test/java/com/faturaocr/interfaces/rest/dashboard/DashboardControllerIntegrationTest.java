package com.faturaocr.interfaces.rest.dashboard;

import com.faturaocr.application.dashboard.DashboardService;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DashboardService dashboardService;

        @Test
        void getStats_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/stats")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getCategoryDistribution_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/categories")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getMonthlyTrend_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/monthly-trend")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getTopSuppliers_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/top-suppliers")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getPendingActions_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/pending-actions")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getStatusTimeline_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("REPORT_VIEW")));

                mockMvc.perform(get("/api/v1/dashboard/status-timeline")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }

        @Test
        void getExtractionPerformance_ShouldReturn200() throws Exception {
                UUID companyId = UUID.randomUUID();
                AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "test@test.com", companyId,
                                "ADMIN");
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ADMIN"))); // Needs ADMIN or MANAGER

                mockMvc.perform(get("/api/v1/dashboard/extraction-performance")
                                .with(authentication(auth)))
                                .andExpect(status().isOk());
        }
}
