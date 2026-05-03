package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.domain.invoice.port.FileStoragePort;

import com.faturaocr.infrastructure.adapter.extraction.PythonExtractionClient;
import com.faturaocr.infrastructure.adapter.extraction.dto.ExtractionResult;
import com.faturaocr.infrastructure.messaging.rabbitmq.RabbitMQProducerService;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InvoiceUploadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private InvoiceJpaRepository invoiceJpaRepository;

        @MockitoBean
        private FileStoragePort fileStoragePort;

        @MockitoBean
        private PythonExtractionClient pythonExtractionClient;

        @MockitoBean
        private RabbitMQProducerService rabbitMQProducerService;

        @MockitoBean
        private com.faturaocr.infrastructure.messaging.rabbitmq.RabbitMQResultListener rabbitMQResultListener;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider; // In case filters need it

        private final UUID userId = UUID.randomUUID();
        private final UUID companyId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                invoiceJpaRepository.deleteAll();

                // Setup Security Context
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "test@example.com", companyId,
                                "ADMIN");
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                authenticatedUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Mock FileStorage
                try {
                        Mockito.when(fileStoragePort.saveFile(any(), anyString(), anyString()))
                                        .thenReturn("/tmp/test-file.pdf");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }

                // Mock Extraction
                ExtractionResult result = new ExtractionResult();
                result.setConfidenceScore(new BigDecimal("95.5"));
                result.setProvider("TEST_LLM");
                ExtractionResult.InvoiceData data = new ExtractionResult.InvoiceData();
                data.setInvoiceNumber("INV-001");
                data.setTotalAmount(new BigDecimal("100.00"));
                data.setInvoiceDate(LocalDate.now());
                data.setSupplierName("Test Supplier");
                result.setInvoiceData(data);

                Mockito.when(pythonExtractionClient.extract(any(Path.class))).thenReturn(result);
        }

        @Test
        @SuppressWarnings("null")
        void shouldUploadSingleInvoiceSuccessfully() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "invoice.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "dummy content".getBytes());

                mockMvc.perform(multipart("/api/v1/invoices/upload")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)) // @SuppressWarnings("null")
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.originalFileName").value("invoice.pdf"))
                                .andExpect(jsonPath("$.status").exists())
                                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
        }

        @Test
        @SuppressWarnings("null")
        void shouldBulkUploadInvoicesSuccessfully() throws Exception {
                MockMultipartFile file1 = new MockMultipartFile(
                                "files",
                                "invoice1.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "dummy content 1".getBytes());

                MockMultipartFile file2 = new MockMultipartFile(
                                "files",
                                "invoice2.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "dummy content 2".getBytes());

                mockMvc.perform(multipart("/api/v1/invoices/bulk-upload")
                                .file(file1)
                                .file(file2)
                                .contentType(MediaType.MULTIPART_FORM_DATA)) // @SuppressWarnings("null")
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalFiles").value(2))
                                .andExpect(jsonPath("$.acceptedFiles").value(2))
                                .andExpect(jsonPath("$.batchId").exists());

                Mockito.verify(rabbitMQProducerService, Mockito.times(2)).publishExtractionRequest(any());
        }
}
