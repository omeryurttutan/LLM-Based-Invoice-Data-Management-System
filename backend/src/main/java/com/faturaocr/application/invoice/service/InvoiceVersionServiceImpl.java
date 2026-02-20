package com.faturaocr.application.invoice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.domain.invoice.dto.InvoiceVersionDto;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.entity.InvoiceVersion;
import com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource;
import com.faturaocr.domain.invoice.repository.InvoiceVersionRepository;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.user.UserJpaEntity;
import com.faturaocr.infrastructure.persistence.user.UserJpaRepository;
import com.faturaocr.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceVersionServiceImpl implements InvoiceVersionService {

    private final InvoiceVersionRepository versionRepository;
    private final InvoiceJpaRepository invoiceJpaRepository;
    private final UserJpaRepository jpaUserRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_VERSIONS_PER_INVOICE = 50;

    private static final Map<String, String> FIELD_LABELS = new HashMap<>();
    static {
        FIELD_LABELS.put("invoiceNumber", "Fatura Numarası");
        FIELD_LABELS.put("invoiceDate", "Fatura Tarihi");
        FIELD_LABELS.put("dueDate", "Vade Tarihi");
        FIELD_LABELS.put("supplierName", "Tedarikçi Adı");
        FIELD_LABELS.put("supplierTaxNumber", "Tedarikçi Vergi No");
        FIELD_LABELS.put("buyerName", "Alıcı Adı");
        FIELD_LABELS.put("buyerTaxNumber", "Alıcı Vergi No");
        FIELD_LABELS.put("subtotal", "Ara Toplam");
        FIELD_LABELS.put("taxAmount", "KDV Tutarı");
        FIELD_LABELS.put("totalAmount", "Genel Toplam");
        FIELD_LABELS.put("currency", "Para Birimi");
        FIELD_LABELS.put("categoryName", "Kategori");
        FIELD_LABELS.put("status", "Durum");
        FIELD_LABELS.put("notes", "Notlar");
        FIELD_LABELS.put("confidenceScore", "Güven Skoru");
        FIELD_LABELS.put("llmProvider", "LLM Sağlayıcı");
    }

    @Override
    @Transactional
    public void createSnapshot(Invoice invoice, List<InvoiceItem> items, ChangeSource source, String changeSummary) {
        // log.info("Creating version snapshot for invoice {} with source {}",
        // invoice.getId(), source);
        // Avoiding log if items is null or large, just ID and source

        try {
            JsonNode snapshotData = objectMapper.valueToTree(invoice);
            JsonNode itemsSnapshot = objectMapper.valueToTree(items);

            Integer maxVersion = versionRepository.findMaxVersionNumberByInvoiceId(invoice.getId())
                    .orElse(0);
            Integer nextVersion = maxVersion + 1;

            InvoiceJpaEntity invoiceEntity = invoiceJpaRepository.findById(invoice.getId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            UUID currentUserId = SecurityUtils.getCurrentUserId();
            UserJpaEntity currentUserEntity = jpaUserRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            InvoiceVersion version = InvoiceVersion.builder()
                    .invoice(invoiceEntity)
                    .versionNumber(nextVersion)
                    .snapshotData(snapshotData)
                    .itemsSnapshot(itemsSnapshot)
                    .changeSource(source)
                    .changeSummary(changeSummary)
                    .changedBy(currentUserEntity)
                    .companyId(invoice.getCompanyId())
                    .build();

            versionRepository.save(version);

            cleanupOldVersions(invoice.getId());

        } catch (Exception e) {
            log.error("Failed to create version snapshot for invoice {}", invoice.getId(), e);
            throw new RuntimeException("Failed to create invoice version", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceVersionDto.Summary> getVersions(UUID invoiceId) {
        return versionRepository.findByInvoiceIdOrderByVersionNumberDesc(invoiceId).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceVersionDto.Detail getVersion(UUID invoiceId, Integer versionNumber) {
        InvoiceVersion version = versionRepository.findByInvoiceIdAndVersionNumber(invoiceId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return mapToDetail(version);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceVersionDto.VersionDiff compareVersions(UUID invoiceId, Integer versionFrom, Integer versionTo) {
        InvoiceVersion vFrom = versionRepository.findByInvoiceIdAndVersionNumber(invoiceId, versionFrom)
                .orElseThrow(() -> new RuntimeException("Version " + versionFrom + " not found"));
        InvoiceVersion vTo = versionRepository.findByInvoiceIdAndVersionNumber(invoiceId, versionTo)
                .orElseThrow(() -> new RuntimeException("Version " + versionTo + " not found"));

        return calculateDiff(vFrom, vTo);
    }

    @Override
    @Transactional
    public Invoice revertToVersion(UUID invoiceId, Integer versionNumber) {
        InvoiceVersion targetVersion = versionRepository.findByInvoiceIdAndVersionNumber(invoiceId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        try {
            Invoice revertedInvoice = objectMapper.treeToValue(targetVersion.getSnapshotData(), Invoice.class);

            JsonNode itemsNode = targetVersion.getItemsSnapshot();
            if (itemsNode != null && itemsNode.isArray()) {
                List<InvoiceItem> items = Arrays.asList(objectMapper.treeToValue(itemsNode, InvoiceItem[].class));
                revertedInvoice.setItems(items);
            }

            return revertedInvoice;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize version snapshot", e);
        }
    }

    private void cleanupOldVersions(UUID invoiceId) {
        List<InvoiceVersion> allVersions = versionRepository.findVersionsByInvoiceId(invoiceId);

        if (allVersions.size() > MAX_VERSIONS_PER_INVOICE) {
            int toDeleteCount = allVersions.size() - MAX_VERSIONS_PER_INVOICE;
            List<InvoiceVersion> toDelete = allVersions.subList(0, toDeleteCount);

            versionRepository.deleteAll(toDelete);
            log.info("Deleted {} old versions for invoice {}", toDelete.size(), invoiceId);
        }
    }

    private InvoiceVersionDto.Summary mapToSummary(InvoiceVersion v) {
        return InvoiceVersionDto.Summary.builder()
                .id(v.getId())
                .versionNumber(v.getVersionNumber())
                .changeSource(v.getChangeSource())
                .changeSummary(v.getChangeSummary())
                .changedBy(mapUser(v.getChangedBy()))
                .createdAt(v.getCreatedAt())
                .build();
    }

    private InvoiceVersionDto.Detail mapToDetail(InvoiceVersion v) {
        return InvoiceVersionDto.Detail.builder()
                .id(v.getId())
                .versionNumber(v.getVersionNumber())
                .changeSource(v.getChangeSource())
                .changeSummary(v.getChangeSummary())
                .snapshotData(v.getSnapshotData())
                .itemsSnapshot(v.getItemsSnapshot())
                .changedBy(mapUser(v.getChangedBy()))
                .createdAt(v.getCreatedAt())
                .build();
    }

    private InvoiceVersionDto.UserDto mapUser(UserJpaEntity user) {
        if (user == null)
            return null;
        return InvoiceVersionDto.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFullName())
                .build();
    }

    private InvoiceVersionDto.VersionDiff calculateDiff(InvoiceVersion vFrom, InvoiceVersion vTo) {
        List<InvoiceVersionDto.FieldChange> changes = new ArrayList<>();

        JsonNode fromData = vFrom.getSnapshotData();
        JsonNode toData = vTo.getSnapshotData();

        Iterator<Map.Entry<String, JsonNode>> fields = toData.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode newValue = field.getValue();
            JsonNode oldValue = fromData.get(fieldName);

            if (shouldIgnoreField(fieldName))
                continue;

            if (!isEqual(oldValue, newValue)) {
                changes.add(InvoiceVersionDto.FieldChange.builder()
                        .fieldName(fieldName)
                        .fieldLabel(FIELD_LABELS.getOrDefault(fieldName, fieldName))
                        .oldValue(getValue(oldValue))
                        .newValue(getValue(newValue))
                        .changeType(InvoiceVersionDto.ChangeType.MODIFIED)
                        .build());
            }
        }

        InvoiceVersionDto.ItemChanges itemChanges = compareItems(vFrom.getItemsSnapshot(), vTo.getItemsSnapshot());

        return InvoiceVersionDto.VersionDiff.builder()
                .fromVersion(vFrom.getVersionNumber())
                .toVersion(vTo.getVersionNumber())
                .fromCreatedAt(vFrom.getCreatedAt())
                .toCreatedAt(vTo.getCreatedAt())
                .changes(changes)
                .itemChanges(itemChanges)
                .build();
    }

    private boolean isEqual(JsonNode a, JsonNode b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }

    private boolean shouldIgnoreField(String fieldName) {
        return Set.of("items", "company", "createdByUserId", "createdAt", "updatedAt", "version", "id", "verifiedAt",
                "rejectedAt", "companyId").contains(fieldName);
    }

    private Object getValue(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        if (node.isTextual())
            return node.asText();
        if (node.isNumber())
            return node.numberValue();
        if (node.isBoolean())
            return node.booleanValue();
        return node.toString();
    }

    private InvoiceVersionDto.ItemChanges compareItems(JsonNode fromItems, JsonNode toItems) {
        List<JsonNode> added = new ArrayList<>();
        List<JsonNode> removed = new ArrayList<>();
        List<InvoiceVersionDto.ItemModification> modified = new ArrayList<>();

        return InvoiceVersionDto.ItemChanges.builder()
                .added(added)
                .removed(removed)
                .modified(modified)
                .build();
    }
}
