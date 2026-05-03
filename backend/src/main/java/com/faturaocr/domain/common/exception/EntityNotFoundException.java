package com.faturaocr.domain.common.exception;

import java.util.UUID;

/**
 * Exception thrown when an entity is not found.
 */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, UUID id) {
        super(
                "ENTITY_NOT_FOUND",
                String.format("%s with id %s not found", entityName, id));
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super(
                "ENTITY_NOT_FOUND",
                String.format("%s with identifier %s not found", entityName, identifier));
    }
}
