package com.fujitsu.events.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "events",
        uniqueConstraints = @UniqueConstraint(name = "uk_events_event_id", columnNames = "event_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public Event(
            String eventId,
            EventType eventType,
            String description,
            LocalDateTime timestamp,
            Instant receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
        this.receivedAt = receivedAt;
    }
}
