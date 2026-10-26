package com.fujitsu.events.dto;

import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventResponse(
        String eventId,
        EventType eventType,
        String description,
        LocalDateTime timestamp,
        Instant receivedAt) {}
