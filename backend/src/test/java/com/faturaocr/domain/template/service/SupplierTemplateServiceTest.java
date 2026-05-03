package com.faturaocr.domain.template.service;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;
import com.faturaocr.domain.template.entity.SupplierTemplate;
import com.faturaocr.domain.template.port.SupplierTemplateRepository;
import com.faturaocr.domain.template.valueobject.CommonCorrection;
import com.faturaocr.domain.template.valueobject.LearnedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierTemplateServiceTest {

    @Mock
    private SupplierTemplateRepository repository;

    @InjectMocks
    private SupplierTemplateService service;

    private UUID companyId;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        invoice = new Invoice();
        invoice.setCompanyId(companyId);
        invoice.setSupplierTaxNumber("1234567890");
        invoice.setSupplierName("Test Supplier");
        invoice.setInvoiceDate(LocalDate.now());

        ReflectionTestUtils.setField(service, "learningEnabled", true);
        ReflectionTestUtils.setField(service, "minSamplesForSuggestion", 3);
    }

    @Test
    void learnFromInvoice_shouldCreateNewTemplate_whenNoneExists() {
        when(repository.findByCompanyIdAndSupplierTaxNumber(companyId, "1234567890"))
                .thenReturn(Optional.empty());

        service.learnFromInvoice(invoice);

        verify(repository).save(any(SupplierTemplate.class));
    }

    @Test
    void learnFromInvoice_shouldUpdateExistingTemplate() {
        SupplierTemplate template = new SupplierTemplate();
        template.setSampleCount(1);
        template.setLearnedData(new LearnedData());
        when(repository.findByCompanyIdAndSupplierTaxNumber(companyId, "1234567890"))
                .thenReturn(Optional.of(template));

        service.learnFromInvoice(invoice);

        assertEquals(2, template.getSampleCount());
        verify(repository).save(template);
    }

    @Test
    void learnFromInvoice_shouldTrackCorrections() {
        SupplierTemplate template = new SupplierTemplate();
        template.setLearnedData(new LearnedData());
        when(repository.findByCompanyIdAndSupplierTaxNumber(companyId, "1234567890"))
                .thenReturn(Optional.of(template));

        List<ExtractionCorrection> corrections = new ArrayList<>();
        ExtractionCorrection ec = new ExtractionCorrection();
        ec.setField("supplier_name");
        ec.setOriginalValue("Old Name");
        ec.setCorrectedValue("Test Supplier");
        corrections.add(ec);
        invoice.setExtractionCorrections(corrections);

        service.learnFromInvoice(invoice);

        assertNotNull(template.getLearnedData().getCommonCorrections());
        assertFalse(template.getLearnedData().getCommonCorrections().isEmpty());
        assertEquals("supplier_name", template.getLearnedData().getCommonCorrections().get(0).getField());
    }

    @Test
    void applyTemplateToInvoice_shouldApplyDefaultCategory() {
        SupplierTemplate template = new SupplierTemplate();
        template.setActive(true);
        template.setSampleCount(5);
        UUID categoryId = UUID.randomUUID();
        template.setDefaultCategoryId(categoryId);

        when(repository.findByCompanyIdAndSupplierTaxNumber(companyId, "1234567890"))
                .thenReturn(Optional.of(template));

        service.applyTemplateToInvoice(invoice);

        assertEquals(categoryId, invoice.getCategoryId());
        assertTrue(invoice.getNotes().contains("[Auto-Suggestion]"));
    }

    @Test
    void applyTemplateToInvoice_shouldApplyCorrections() {
        SupplierTemplate template = new SupplierTemplate();
        template.setActive(true);
        template.setSampleCount(5);

        LearnedData data = new LearnedData();
        CommonCorrection cc = CommonCorrection.builder()
                .field("supplier_name")
                .originalValue("WRONG NAME")
                .correctedTo("Test Supplier")
                .frequency(5)
                .build();
        data.getCommonCorrections().add(cc);
        template.setLearnedData(data);

        when(repository.findByCompanyIdAndSupplierTaxNumber(companyId, "1234567890"))
                .thenReturn(Optional.of(template));

        invoice.setSupplierName("WRONG NAME");

        service.applyTemplateToInvoice(invoice);

        assertEquals("Test Supplier", invoice.getSupplierName());
    }
}
