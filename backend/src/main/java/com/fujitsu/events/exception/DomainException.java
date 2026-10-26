package com.fujitsu.events.exception;

import lombok.Getter;

@Getter
public abstract sealed class DomainException extends RuntimeException
        permits DuplicateEventException, EventNotFoundException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
