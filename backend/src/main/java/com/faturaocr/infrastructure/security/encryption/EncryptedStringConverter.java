package com.faturaocr.infrastructure.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    // Default constructor is required by JPA.
    // Spring will inject the dependency if used as a Component, but for JPA
    // Converter it's tricky.
    // In Spring Boot, AttributeConverters are managed beans if annotated with
    // @Component,
    // so constructor injection should work for the singleton instance used by
    // Hibernate.
    public EncryptedStringConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        // If it's already encrypted (e.g. during migration or check), we might want to
        // avoid double encryption.
        // But for now, assume attribute is ALWAYS plain text coming from entity.
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService.decrypt(dbData);
    }
}
