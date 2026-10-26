package com.fujitsu.events.exception;

public final class EventNotFoundException extends DomainException {

    public EventNotFoundException(String eventId) {
        super(ErrorCode.EVENT_NOT_FOUND, "No existe un evento con eventId '%s'.".formatted(eventId));
    }
}
