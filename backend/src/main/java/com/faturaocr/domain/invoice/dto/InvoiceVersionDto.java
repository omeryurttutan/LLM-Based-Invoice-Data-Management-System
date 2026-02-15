package com.faturaocr.domain.invoice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InvoiceVersionDto {

    @Data
    @Builder
    public static class Summary {
        private UUID id;
        private Integer versionNumber;
        private ChangeSource changeSource;
        private String changeSummary;
        private List<String> changedFields;
        private UserDto changedBy;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class Detail {
        private UUID id;
        private Integer versionNumber;
        private ChangeSource changeSource;
        private String changeSummary;
        private JsonNode snapshotData;
        private JsonNode itemsSnapshot;
        private UserDto changedBy;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class UserDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    @Builder
    public static class VersionDiff {
        private Integer fromVersion;
        private Integer toVersion;
        private LocalDateTime fromCreatedAt;
        private LocalDateTime toCreatedAt;
        private List<FieldChange> changes;
        private ItemChanges itemChanges;
    }

    @Data
    @Builder
    public static class FieldChange {
        private String fieldName;
        private String fieldLabel;
        private Object oldValue;
        private Object newValue;
        private ChangeType changeType;
    }

    @Data
    @Builder
    public static class ItemChanges {
        private List<JsonNode> added;
        private List<JsonNode> removed;
        private List<ItemModification> modified;
    }

    @Data
    @Builder
    public static class ItemModification {
        private int index;
        private List<FieldChange> changes;
        private JsonNode oldItem;
        private JsonNode newItem;
    }

    public enum ChangeType {
        MODIFIED, ADDED, REMOVED
    }
}
