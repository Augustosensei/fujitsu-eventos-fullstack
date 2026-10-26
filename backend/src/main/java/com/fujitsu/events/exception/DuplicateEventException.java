package com.fujitsu.events.exception;

public final class DuplicateEventException extends DomainException {

    public DuplicateEventException(String eventId) {
        super(
                ErrorCode.DUPLICATE_EVENT,
                "El evento con eventId '%s' ya estaba registrado; no se guardó ni se auditó de nuevo."
                        .formatted(eventId));
    }
}
