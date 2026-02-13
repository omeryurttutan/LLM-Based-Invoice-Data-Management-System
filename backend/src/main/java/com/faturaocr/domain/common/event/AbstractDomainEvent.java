package com.faturaocr.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for domain events.
 */
public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final LocalDateTime occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
