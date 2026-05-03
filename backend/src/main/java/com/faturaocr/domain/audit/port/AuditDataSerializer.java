package com.faturaocr.domain.audit.port;

import java.util.Map;

/**
 * Port interface for audit data serialization and deserialization.
 */
public interface AuditDataSerializer {

    /**
     * Deserialize JSON string to a map of field names to values.
     */
    Map<String, Object> deserializeToMap(String json);
}
