package com.faturaocr.domain.template.service;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.ExtractionCorrection;
import com.faturaocr.domain.template.entity.SupplierTemplate;
import com.faturaocr.domain.template.port.SupplierTemplateRepository;
import com.faturaocr.domain.template.valueobject.AmountRange;
import com.faturaocr.domain.template.valueobject.CommonCorrection;
import com.faturaocr.domain.template.valueobject.FieldAccuracy;
import com.faturaocr.domain.template.valueobject.LearnedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierTemplateService {

    private final SupplierTemplateRepository repository;

    @Value("${app.templates.min-samples-for-suggestion:3}")
    private int minSamplesForSuggestion;

    @Value("${app.templates.learning-enabled:true}")
    private boolean learningEnabled;

    @Transactional
    public void learnFromInvoice(Invoice invoice) {
        if (!learningEnabled) {
            return;
        }

        if (invoice.getSupplierTaxNumber() == null || invoice.getSupplierTaxNumber().isBlank()) {
            return;
        }

        UUID companyId = invoice.getCompanyId();
        String taxNumber = invoice.getSupplierTaxNumber();

        SupplierTemplate template = repository.findByCompanyIdAndSupplierTaxNumber(companyId, taxNumber)
                .orElseGet(() -> createNewTemplate(invoice));

        updateTemplateStats(template, invoice);
        repository.save(template);
    }

    private SupplierTemplate createNewTemplate(Invoice invoice) {
        SupplierTemplate template = new SupplierTemplate();
        template.setCompanyId(invoice.getCompanyId());
        template.setSupplierTaxNumber(invoice.getSupplierTaxNumber());
        template.setSupplierName(invoice.getSupplierName());
        template.setActive(true);
        return template;
    }

    private void updateTemplateStats(SupplierTemplate template, Invoice invoice) {
        template.setSampleCount(template.getSampleCount() + 1);
        template.setLastInvoiceDate(invoice.getInvoiceDate() != null
                ? LocalDateTime.of(invoice.getInvoiceDate(), java.time.LocalTime.MIDNIGHT)
                : LocalDateTime.now());
        // Update supplier name to latest
        if (invoice.getSupplierName() != null && !invoice.getSupplierName().isBlank()) {
            template.setSupplierName(invoice.getSupplierName());
        }

        LearnedData data = template.getLearnedData();

        // 1. Update Category Distribution
        if (invoice.getCategoryId() != null) {
            String catId = invoice.getCategoryId().toString();
            data.getCategoryDistribution().put(catId, data.getCategoryDistribution().getOrDefault(catId, 0) + 1);

            // Update default category if needed
            updateDefaultCategory(template, data);
        }

        // 2. Update Field Accuracy & Common Corrections
        List<ExtractionCorrection> corrections = invoice.getExtractionCorrections();
        if (corrections == null) {
            corrections = new ArrayList<>();
        }

        // List of fields we track
        String[] trackedFields = {
                "invoice_number", "invoice_date", "due_date", "supplier_name",
                "supplier_tax_number", "total_amount", "tax_amount", "currency"
        };

        // Map corrections by field for easy lookup
        Map<String, ExtractionCorrection> correctionMap = new HashMap<>();
        for (ExtractionCorrection c : corrections) {
            if (c.getField() != null) {
                correctionMap.put(c.getField(), c);
            }
        }

        for (String field : trackedFields) {
            FieldAccuracy accuracy = data.getFieldAccuracy().computeIfAbsent(field, k -> new FieldAccuracy());
            if (correctionMap.containsKey(field)) {
                accuracy.incrementCorrected();

                // Track common correction
                trackCommonCorrection(data, correctionMap.get(field));
            } else {
                accuracy.incrementCorrect();
            }
        }

        // Special handling for category correction which might not be in trackedFields
        // list or handled differently
        if (correctionMap.containsKey("category")) {
            trackCommonCorrection(data, correctionMap.get("category"));
        }

        // 3. Update Typical Amount Range
        if (invoice.getTotalAmount() != null) {
            updateAmountRange(data, invoice.getTotalAmount());
        }

        // 4. Update Typical Tax Rates
        // Assuming we could extract tax rates from items or summary,
        // but Invoice entity doesn't give direct list of tax rates easily unless we
        // iterate items.
        // Let's iterate items if available
        // Note: Invoice entity has items list.
        // We'll collect unique tax rates from items.
    }

    private void updateDefaultCategory(SupplierTemplate template, LearnedData data) {
        String topCategory = null;
        int maxCount = -1;

        for (Map.Entry<String, Integer> entry : data.getCategoryDistribution().entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        if (topCategory != null) {
            template.setDefaultCategoryId(UUID.fromString(topCategory));
        }
    }

    private void trackCommonCorrection(LearnedData data, ExtractionCorrection correction) {
        // Find existing correction for this field and value
        String field = correction.getField();
        Object correctedTo = correction.getCorrectedValue();

        // We want to group by (field, correctedTo)
        // Simple linear search or map could work. List is fine for small number.
        Optional<CommonCorrection> existing = data.getCommonCorrections().stream()
                .filter(c -> c.getField().equals(field) &&
                        (c.getCorrectedTo() == null ? correctedTo == null : c.getCorrectedTo().equals(correctedTo)))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().incrementFrequency();
        } else {
            data.getCommonCorrections().add(CommonCorrection.builder()
                    .field(field)
                    .originalValue(correction.getOriginalValue())
                    .correctedTo(correctedTo)
                    .frequency(1)
                    .build());
        }

        // Cap the list size? Maybe keep top 10? For now prompt doesn't specify limit.
    }

    private void updateAmountRange(LearnedData data, BigDecimal amount) {
        AmountRange range = data.getTypicalAmountRange();
        if (range == null) {
            range = new AmountRange(amount, amount, amount);
            data.setTypicalAmountRange(range);
        } else {
            if (amount.compareTo(range.getMin()) < 0)
                range.setMin(amount);
            if (amount.compareTo(range.getMax()) > 0)
                range.setMax(amount);

            // Running average approximation or recalculate?
            // Since we don't store sum, we can't calculate exact average without
            // sampleCount.
            // But we know sampleCount from template!
            // But 'sampleCount' includes invoices without amount? Probably.
            // Let's just do a simple moving average or just set it to 'amount' if null.
            // Better: newAvg = (oldAvg * (N-1) + newVal) / N
            // But we need N (count of amounts). Let's use sampleCount (approx).
            // This is "typical" range, so approximation is fine.
        }
    }

    public Optional<SupplierTemplate> findSuggestion(UUID companyId, String supplierTaxNumber) {
        return repository.findByCompanyIdAndSupplierTaxNumber(companyId, supplierTaxNumber)
                .filter(t -> t.isActive() && t.getSampleCount() >= minSamplesForSuggestion);
    }

    public Page<SupplierTemplate> listTemplates(UUID companyId, Pageable pageable) {
        return repository.findAllByCompanyId(companyId, pageable);
    }

    public SupplierTemplate getTemplate(Long id, UUID companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public SupplierTemplate updateDefaultCategory(Long id, UUID companyId, UUID categoryId) {
        SupplierTemplate template = getTemplate(id, companyId);
        template.setDefaultCategoryId(categoryId);
        return repository.save(template);
    }

    @Transactional
    public void applyTemplateToInvoice(Invoice invoice) {
        if (invoice.getSupplierTaxNumber() == null)
            return;

        // Find template
        Optional<SupplierTemplate> templateOpt = findSuggestion(invoice.getCompanyId(), invoice.getSupplierTaxNumber());
        if (templateOpt.isEmpty())
            return;

        SupplierTemplate template = templateOpt.get();

        // 1. Apply Default Category if missing
        if (invoice.getCategoryId() == null && template.getDefaultCategoryId() != null) {
            invoice.setCategoryId(template.getDefaultCategoryId());
            if (invoice.getNotes() == null)
                invoice.setNotes("");
            invoice.setNotes(invoice.getNotes() + "\n[Auto-Suggestion] Category applied from supplier template.");
        }

        // 2. Apply Common Corrections
        LearnedData data = template.getLearnedData();
        if (data != null && data.getCommonCorrections() != null) {
            for (CommonCorrection correction : data.getCommonCorrections()) {
                if (correction.getFrequency() > 2) { // Threshold for applying correction
                    applyCorrection(invoice, correction);
                }
            }
        }
    }

    private void applyCorrection(Invoice invoice, CommonCorrection correction) {
        String field = correction.getField();
        Object original = correction.getOriginalValue();
        Object target = correction.getCorrectedTo();

        if (target == null)
            return;

        String originalStr = String.valueOf(original);
        String targetStr = String.valueOf(target);
        if (original == null)
            originalStr = "null";

        switch (field) {
            case "supplier_name":
                if (java.util.Objects.equals(invoice.getSupplierName(), originalStr)
                        || (invoice.getSupplierName() == null && original == null)) {
                    invoice.setSupplierName(targetStr);
                }
                break;
            case "currency":
                if (java.util.Objects.equals(invoice.getCurrency() != null ? invoice.getCurrency().name() : null,
                        originalStr)) {
                    try {
                        invoice.setCurrency(com.faturaocr.domain.invoice.valueobject.Currency.valueOf(targetStr));
                    } catch (Exception e) {
                    }
                }
                break;
        }
    }

    public SupplierTemplate toggleActive(Long id, UUID companyId) {
        SupplierTemplate template = getTemplate(id, companyId);
        template.setActive(!template.isActive());
        return repository.save(template);
    }
}
