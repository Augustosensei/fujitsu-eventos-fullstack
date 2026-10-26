package com.fujitsu.events.model;

public enum EventType {
    PRODUCTION,
    QUALITY;

    public boolean requiresAudit() {
        return switch (this) {
            case PRODUCTION -> false;
            case QUALITY -> true;
        };
    }
}
