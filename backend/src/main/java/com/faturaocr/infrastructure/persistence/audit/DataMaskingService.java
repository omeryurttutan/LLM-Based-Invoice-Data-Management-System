package com.faturaocr.infrastructure.persistence.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.faturaocr.domain.audit.annotation.AuditMask;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.user.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMaskingService {

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, AuditMask.MaskType>> entityMaskingRules = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register entities to scan
        registerEntity(User.class);
        registerEntity(Company.class);
        registerEntity(Invoice.class);
        // Add other entities if needed
    }

    private void registerEntity(Class<?> clazz) {
        Map<String, AuditMask.MaskType> fieldRules = new HashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(AuditMask.class)) {
                    AuditMask annotation = field.getAnnotation(AuditMask.class);
                    fieldRules.put(field.getName(), annotation.value());
                }
            }
            current = current.getSuperclass();
        }

        if (!fieldRules.isEmpty()) {
            // Register specific class name
            entityMaskingRules.put(clazz.getSimpleName(), fieldRules);
            entityMaskingRules.put(clazz.getName(), fieldRules);
            // Also register variations if needed (e.g. "users" table name vs "User" entity
            // name)
            // But AuditLog entityType is usually class name or table name.
            // Adjust based on observation of audit logs.
        }
    }

    public String mask(String entityType, String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty() || entityType == null) {
            return jsonContent;
        }

        // Try to match entity type
        Map<String, AuditMask.MaskType> rules = entityMaskingRules.get(entityType);
        // Fallback: try case-insensitive or partial match?
        if (rules == null) {
            // Try to find by simple name if full name passed, or vice versa
            // For now, strict match.
            return jsonContent;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (root.isObject()) {
                maskObject((ObjectNode) root, rules);
                return objectMapper.writeValueAsString(root);
            }
        } catch (Exception e) {
            log.warn("Failed to mask sensitive data for entity {}: {}", entityType, e.getMessage());
            // Return original if parsing fails (might not be JSON)
        }
        return jsonContent;
    }

    private void maskObject(ObjectNode node, Map<String, AuditMask.MaskType> rules) {
        rules.forEach((fieldName, maskType) -> {
            if (node.has(fieldName) && node.get(fieldName).isTextual()) {
                String original = node.get(fieldName).asText();
                node.set(fieldName, new TextNode(applyMask(original, maskType)));
            }
        });
    }

    private String applyMask(String value, AuditMask.MaskType type) {
        if (value == null)
            return null;
        if (value.isEmpty())
            return "";

        switch (type) {
            case FULL:
                return "[MASKED]";
            case PARTIAL:
                if (value.length() <= 4)
                    return "****";
                return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
            case EMAIL:
                int atIndex = value.indexOf('@');
                if (atIndex <= 1)
                    return "****" + (atIndex >= 0 ? value.substring(atIndex) : "");
                return value.charAt(0) + "***" + value.substring(atIndex);
            case PHONE:
                // Simple phone masking: keep last 2 digits
                if (value.length() <= 4)
                    return "****";
                return "****" + value.substring(value.length() - 2);
            default:
                return "[MASKED]";
        }
    }
}
