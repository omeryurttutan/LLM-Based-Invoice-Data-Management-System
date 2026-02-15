package com.faturaocr.application.invoice;

import com.faturaocr.application.invoice.dto.CreateInvoiceCommand;
import com.faturaocr.application.invoice.dto.DuplicateCheckResult;
import com.faturaocr.application.invoice.dto.InvoiceResponse;
import com.faturaocr.application.invoice.InvoiceDTOMapper;
import com.faturaocr.application.invoice.service.InvoiceVersionService;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.DuplicateConfidence;
import com.faturaocr.domain.notification.service.NotificationService;
import com.faturaocr.domain.rule.service.RuleEngine;
import com.faturaocr.domain.template.service.SupplierTemplateService;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

        @Mock
        private InvoiceRepository invoiceRepository;
        @Mock
        private InvoiceJpaRepository invoiceJpaRepository;
        @Mock
        private CategoryRepository categoryRepository;
        @Mock
        private DuplicateDetectionService duplicateDetectionService;
        @Mock
        private InvoiceDTOMapper mapper;
        @Mock
        private NotificationService notificationService;
        @Mock
        private InvoiceVersionService versionService;
        @Mock
        private SupplierTemplateService supplierTemplateService;
        @Mock
        private RuleEngine ruleEngine;

        @InjectMocks
        private InvoiceService invoiceService;

        private MockedStatic<CompanyContextHolder> companyContextHolderMock;

        @BeforeEach
        void setUp() {
                companyContextHolderMock = Mockito.mockStatic(CompanyContextHolder.class);
                companyContextHolderMock.when(CompanyContextHolder::getCompanyId).thenReturn(TestFixtures.COMPANY_ID);

                // Setup SecurityContext
                // AuthenticatedUser record: userId, email, companyId, role
                org.springframework.security.core.context.SecurityContextHolder.setContext(
                                new org.springframework.security.core.context.SecurityContextImpl(
                                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                                                new com.faturaocr.infrastructure.security.AuthenticatedUser(
                                                                                TestFixtures.USER_ID, "user",
                                                                                TestFixtures.COMPANY_ID,
                                                                                com.faturaocr.domain.user.valueobject.Role.ACCOUNTANT
                                                                                                .name()),
                                                                null, Collections.emptyList())));
        }

        @AfterEach
        void tearDown() {
                companyContextHolderMock.close();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should create invoice successfully")
        void shouldCreateInvoiceSuccessfully() {
                // Given
                CreateInvoiceCommand.CreateInvoiceItemCommand itemCmd = CreateInvoiceCommand.CreateInvoiceItemCommand
                                .builder()
                                .description("Item 1")
                                .quantity(new BigDecimal("1"))
                                .unit("ADET")
                                .unitPrice(new BigDecimal("100"))
                                .taxRate(new BigDecimal("18"))
                                .build();

                CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                                .invoiceNumber("INV-100")
                                .invoiceDate(LocalDate.now())
                                .dueDate(LocalDate.now().plusDays(7))
                                .supplierName("Supplier A")
                                .supplierTaxNumber("1111111111")
                                .supplierAddress("Addr")
                                .supplierPhone("Phone")
                                .supplierEmail("Email")
                                .currency("TRY")
                                .exchangeRate(BigDecimal.ONE)
                                .notes("Notes")
                                .items(List.of(itemCmd))
                                .build();

                when(duplicateDetectionService.checkForDuplicates(any())).thenReturn(
                                DuplicateCheckResult.builder().hasDuplicates(false).build());

                when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
                        Invoice inv = i.getArgument(0);
                        inv.setId(UUID.randomUUID());
                        return inv;
                });

                // When
                InvoiceResponse response = invoiceService.createInvoice(command, false);

                // Then
                assertThat(response.getId()).isNotNull();
                verify(invoiceRepository).save(any(Invoice.class));
                verify(ruleEngine).evaluateAndExecute(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when creating invoice with no items")
        void shouldThrowExceptionWhenCreatingInvoiceWithNoItems() {
                // Given
                CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                                .invoiceNumber("INV-101")
                                .invoiceDate(LocalDate.now())
                                .supplierName("Sup")
                                .supplierTaxNumber("111")
                                .currency("TRY")
                                .items(Collections.emptyList())
                                .build();

                // When / Then
                assertThat(org.assertj.core.api.Assertions
                                .catchThrowable(() -> invoiceService.createInvoice(command, false)))
                                .isInstanceOf(com.faturaocr.application.common.exception.BusinessException.class)
                                .hasMessageContaining("At least one item required");
        }

        @Test
        @DisplayName("Should detect duplicates and throw exception")
        void shouldDetectDuplicatesAndThrowException() {
                // Given
                CreateInvoiceCommand.CreateInvoiceItemCommand itemCmd = CreateInvoiceCommand.CreateInvoiceItemCommand
                                .builder()
                                .description("Item")
                                .quantity(BigDecimal.ONE)
                                .unit("ADET")
                                .unitPrice(BigDecimal.TEN)
                                .taxRate(BigDecimal.ZERO)
                                .build();

                CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                                .invoiceNumber("INV-DUP")
                                .invoiceDate(LocalDate.now())
                                .supplierName("Sup")
                                .supplierTaxNumber("111")
                                .currency("TRY")
                                .items(List.of(itemCmd))
                                .build();

                when(duplicateDetectionService.checkForDuplicates(any())).thenReturn(
                                DuplicateCheckResult.builder()
                                                .hasDuplicates(true)
                                                .highestConfidence(DuplicateConfidence.HIGH)
                                                .build());

                // When / Then
                assertThat(org.assertj.core.api.Assertions
                                .catchThrowable(() -> invoiceService.createInvoice(command, false)))
                                .isInstanceOf(com.faturaocr.application.common.exception.DuplicateInvoiceException.class);
        }

        @Test
        @DisplayName("Should verify invoice successfully")
        void shouldVerifyInvoiceSuccessfully() {
                // Given
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = TestFixtures.defaultInvoice();
                invoice.setId(invoiceId);
                invoice.setStatus(com.faturaocr.domain.invoice.valueobject.InvoiceStatus.PENDING);

                when(invoiceRepository.findByIdAndCompanyId(invoiceId, TestFixtures.COMPANY_ID))
                                .thenReturn(java.util.Optional.of(invoice));

                // When
                invoiceService.verifyInvoice(invoiceId, null);

                // Then
                assertThat(invoice.getStatus())
                                .isEqualTo(com.faturaocr.domain.invoice.valueobject.InvoiceStatus.VERIFIED);
                assertThat(invoice.getVerifiedByUserId()).isEqualTo(TestFixtures.USER_ID);
                verify(invoiceRepository, Mockito.times(2)).save(invoice);
                verify(supplierTemplateService).learnFromInvoice(invoice);
                verify(notificationService).notify(any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should reject invoice successfully")
        void shouldRejectInvoiceSuccessfully() {
                // Given
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = TestFixtures.defaultInvoice();
                invoice.setId(invoiceId);

                when(invoiceRepository.findByIdAndCompanyId(invoiceId, TestFixtures.COMPANY_ID))
                                .thenReturn(java.util.Optional.of(invoice));

                // When
                invoiceService.rejectInvoice(invoiceId,
                                new com.faturaocr.application.invoice.dto.RejectInvoiceCommand("Bad data"));

                // Then
                assertThat(invoice.getStatus())
                                .isEqualTo(com.faturaocr.domain.invoice.valueobject.InvoiceStatus.REJECTED);
                assertThat(invoice.getRejectionReason()).isEqualTo("Bad data");
                verify(invoiceRepository).save(invoice);
                verify(notificationService).notify(any(), any(), any(), any(), any(), any(), any(), any(), any());
        }
}
