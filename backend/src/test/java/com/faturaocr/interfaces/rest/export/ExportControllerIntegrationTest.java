package com.faturaocr.interfaces.rest.export;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.ExportService;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportService exportService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void exportInvoices_ShouldReturnXlsx_WhenFormatIsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "XLSX")
                .param("includeItems", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment; filename=\"faturalar_")));

        verify(exportService).exportInvoices(eq(ExportFormat.XLSX), any(Specification.class), eq(false),
                any(OutputStream.class));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void exportInvoices_ShouldReturnCsv_WhenFormatIsCsv() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "CSV")
                .param("includeItems", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment; filename=\"faturalar_")));

        verify(exportService).exportInvoices(eq(ExportFormat.CSV), any(Specification.class), eq(true),
                any(OutputStream.class));
    }

    @Test
    @WithMockUser(authorities = "INTERN") // INTERN should not have access
    void exportInvoices_ShouldReturnForbidden_WhenUserIsIntern() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "XLSX"))
                .andExpect(status().isForbidden());
    }
}
