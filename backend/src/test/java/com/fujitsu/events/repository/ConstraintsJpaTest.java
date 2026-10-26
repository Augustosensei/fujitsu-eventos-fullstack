package com.fujitsu.events.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fujitsu.events.model.AuditRecord;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ConstraintsJpaTest {

    private static final Instant NOW = Instant.parse("2026-05-20T13:00:00Z");
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 5, 20, 10, 0);

    private final EventRepository eventRepository;
    private final AuditRecordRepository auditRecordRepository;

    ConstraintsJpaTest(EventRepository eventRepository, AuditRecordRepository auditRecordRepository) {
        this.eventRepository = eventRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    @Test
    @DisplayName("AU-05 un segundo Event con el mismo eventId viola uk_events_event_id")
    void givenStoredEvent_whenSavingSameEventId_thenUniqueConstraintIsViolated() {
        eventRepository.saveAndFlush(event("EVT-1001", EventType.PRODUCTION));

        assertThatThrownBy(() -> eventRepository.saveAndFlush(event("EVT-1001", EventType.QUALITY)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        violation -> assertThat(violation.getConstraintName())
                                .startsWith("PUBLIC.UK_EVENTS_EVENT_ID INDEX PUBLIC.UK_EVENTS_EVENT_ID_INDEX_"));
    }

    @Test
    @DisplayName("AU-04 una segunda auditoría del mismo evento viola uk_audit_records_event_id")
    void givenAuditedEvent_whenSavingSecondAudit_thenUniqueConstraintIsViolated() {
        var event = eventRepository.saveAndFlush(event("EVT-2002", EventType.QUALITY));
        auditRecordRepository.saveAndFlush(new AuditRecord(event, NOW));

        assertThatThrownBy(() -> auditRecordRepository.saveAndFlush(new AuditRecord(event, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        violation -> assertThat(violation.getConstraintName())
                                .startsWith(
                                        "PUBLIC.UK_AUDIT_RECORDS_EVENT_ID INDEX PUBLIC.UK_AUDIT_RECORDS_EVENT_ID_INDEX_"));
    }

    private static Event event(String eventId, EventType type) {
        return new Event(eventId, type, "Description", TIMESTAMP, NOW);
    }
}
