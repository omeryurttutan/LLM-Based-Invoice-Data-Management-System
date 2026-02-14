package com.faturaocr.infrastructure.audit;

import com.faturaocr.domain.audit.annotation.AuditExclude;
import com.faturaocr.domain.audit.port.AuditDataSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes domain entities to JSON for audit log old_value/new_value fields.
 * Excludes fields annotated with @AuditExclude and truncates long strings.
 */
@Component
public class AuditSerializer implements AuditDataSerializer {

    private static final Logger logger = LoggerFactory.getLogger(AuditSerializer.class);

    private final ObjectMapper objectMapper;

    public AuditSerializer() {
        this.objectMapper = createObjectMapper();
    }

    /**
     * Serialize an object to a JSON string for audit logging.
     * Returns null if the object is null or serialization fails.
     */
    public String serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return truncateJsonValues(json);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize object for audit log: {}", e.getMessage());
            return "{\"error\": \"serialization_failed\"}";
        }
    }

    /**
     * Deserialize a JSON string to a Map for diff computation.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserializeToMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize audit JSON: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Use field visibility for domain entities (they may not have standard getters)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

        // Custom introspector to skip @AuditExclude fields
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public boolean hasIgnoreMarker(com.fasterxml.jackson.databind.introspect.AnnotatedMember m) {
                if (m instanceof AnnotatedField) {
                    AuditExclude exclude = m.getAnnotation(AuditExclude.class);
                    if (exclude != null) {
                        return true;
                    }
                }
                return super.hasIgnoreMarker(m);
            }
        });

        return mapper;
    }

    private String truncateJsonValues(String json) {
        if (json != null && json.length() > 10000) {
            // For very large JSON, truncate the entire string
            return json.substring(0, 10000) + "...(truncated)";
        }
        return json;
    }
}
