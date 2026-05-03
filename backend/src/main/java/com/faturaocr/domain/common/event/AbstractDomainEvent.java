package com.faturaocr.domain.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;

/**
 * Abstract base class for domain events.
 */
@Getter
public abstract class AbstractDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final LocalDateTime occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
