package com.fujitsu.events.service;

import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import com.fujitsu.events.exception.DuplicateEventException;
import com.fujitsu.events.exception.EventNotFoundException;
import com.fujitsu.events.mapper.EventMapper;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.repository.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    static final String REGISTERED_METRIC = "events.registered";
    static final String DUPLICATES_METRIC = "events.duplicates";
    static final String EVENT_ID_CONSTRAINT = "uk_events_event_id";

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "receivedAt", "id");

    private final EventRepository eventRepository;
    private final AuditService auditService;
    private final EventMapper eventMapper;
    private final Paginator paginator;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Transactional
    public EventResponse register(EventRequest request) {
        if (eventRepository.existsByEventId(request.eventId())) {
            throw rejectDuplicate(request.eventId());
        }
        var saved = insert(eventMapper.toEntity(request, effectiveTimestamp(request), clock.instant()));
        if (saved.getEventType().requiresAudit()) {
            auditService.audit(saved);
        }
        meterRegistry.counter(REGISTERED_METRIC, "type", saved.getEventType().name()).increment();
        log.info("Event registered: eventId={}, type={}", saved.getEventId(), saved.getEventType());
        return eventMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> findPage(PageQuery query) {
        var page = paginator.fetch(query, NEWEST_FIRST, eventRepository::findAll, eventRepository::count);
        return PageResponse.from(page.map(eventMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public EventResponse findByEventId(String eventId) {
        return eventRepository
                .findByEventId(eventId)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private LocalDateTime effectiveTimestamp(EventRequest request) {
        return Objects.requireNonNullElseGet(
                request.timestamp(), () -> LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
    }

    private Event insert(Event event) {
        try {
            return eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException exception) {
            if (violatesEventIdConstraint(exception)) {
                throw rejectDuplicate(event.getEventId());
            }
            throw exception;
        }
    }

    private static boolean violatesEventIdConstraint(DataIntegrityViolationException exception) {
        return exception.getCause() instanceof ConstraintViolationException violation
                && violation.getConstraintName() != null
                && violation.getConstraintName().toLowerCase(Locale.ROOT).contains(EVENT_ID_CONSTRAINT);
    }

    private DuplicateEventException rejectDuplicate(String eventId) {
        meterRegistry.counter(DUPLICATES_METRIC).increment();
        log.warn("Duplicate event rejected: eventId={}", eventId);
        return new DuplicateEventException(eventId);
    }
}
