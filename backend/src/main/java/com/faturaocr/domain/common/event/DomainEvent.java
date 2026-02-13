package com.faturaocr.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for all domain events.
 * Domain events represent something that happened in the domain.
 */
public interface DomainEvent {

    UUID getEventId();

    LocalDateTime getOccurredAt();

    String getEventType();
}
