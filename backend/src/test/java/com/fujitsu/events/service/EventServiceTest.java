package com.fujitsu.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.exception.DuplicateEventException;
import com.fujitsu.events.exception.ErrorCode;
import com.fujitsu.events.exception.EventNotFoundException;
import com.fujitsu.events.mapper.AuditMapperImpl;
import com.fujitsu.events.mapper.EventMapperImpl;
import com.fujitsu.events.model.AuditRecord;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.model.EventType;
import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-20T13:00:00.789Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final String EVENT_ID = "EVT-1001";
    private static final String DUPLICATE_MESSAGE =
            "El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo.";
    private static final String REAL_H2_EVENT_ID_CONSTRAINT =
            "PUBLIC.UK_EVENTS_EVENT_ID INDEX PUBLIC.UK_EVENTS_EVENT_ID_INDEX_7";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AuditRecordRepository auditRecordRepository;

    private SimpleMeterRegistry meterRegistry;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var paginator = new Paginator(new PaginationProperties(20, 100));
        var auditService = new AuditService(auditRecordRepository, new AuditMapperImpl(), paginator, CLOCK);
        eventService = new EventService(
                eventRepository, auditService, new EventMapperImpl(), paginator, CLOCK, meterRegistry);
    }

    @Test
    @DisplayName("EV-01 PRODUCTION nuevo se guarda una vez, sin auditoría, con receivedAt del servidor")
    void givenNewProductionEvent_whenRegistering_thenSavedOnceWithoutAudit() {
        givenEventIdIsNew();

        var response = eventService.register(request(EVENT_ID, EventType.PRODUCTION, TIMESTAMP));

        assertThat(response).isEqualTo(new EventResponse(
                EVENT_ID, EventType.PRODUCTION, "Line started", TIMESTAMP, CLOCK.instant()));
        verify(eventRepository).saveAndFlush(any(Event.class));
        verifyNoInteractions(auditRecordRepository);
    }

    @Test
    @DisplayName("EV-02 QUALITY nuevo se guarda y genera una auditoría del mismo evento")
    void givenNewQualityEvent_whenRegistering_thenAuditRecordIsCreatedForThatEvent() {
        givenEventIdIsNew();
        when(auditRecordRepository.saveAndFlush(any(AuditRecord.class))).then(returnsFirstArg());

        eventService.register(request(EVENT_ID, EventType.QUALITY, TIMESTAMP));

        var audit = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).saveAndFlush(audit.capture());
        assertThat(audit.getValue().getEvent().getEventId()).isEqualTo(EVENT_ID);
        assertThat(audit.getValue().getAuditedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("EV-03 reenvío de un eventId PRODUCTION ya registrado → duplicado sin guardar")
    void givenRegisteredProductionEventId_whenRegisteringAgain_thenDuplicateAndNothingSaved() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.register(request(EVENT_ID, EventType.PRODUCTION, TIMESTAMP)))
                .isInstanceOfSatisfying(DuplicateEventException.class, this::assertDuplicate);

        assertNothingPersisted();
    }

    @Test
    @DisplayName("EV-04 reenvío de un eventId QUALITY ya registrado → duplicado sin segunda auditoría")
    void givenRegisteredQualityEventId_whenRegisteringAgain_thenDuplicateAndNoSecondAudit() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.register(request(EVENT_ID, EventType.QUALITY, TIMESTAMP)))
                .isInstanceOfSatisfying(DuplicateEventException.class, this::assertDuplicate);

        assertNothingPersisted();
    }

    @Test
    @DisplayName("EV-05 mismo eventId con otro tipo y descripción → duplicado (la identidad es solo el eventId)")
    void givenRegisteredEventId_whenRegisteringWithOtherTypeAndDescription_thenDuplicate() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(true);
        var otherContent = new EventRequest(EVENT_ID, EventType.QUALITY, "Otra descripción", null);

        assertThatThrownBy(() -> eventService.register(otherContent))
                .isInstanceOf(DuplicateEventException.class);

        assertNothingPersisted();
    }

    @Test
    @DisplayName("EV-06 eventId con espacios alrededor de uno existente se normaliza y es duplicado")
    void givenEventIdWithSpacesOfExistingOne_whenRegistering_thenNormalizedAndDuplicate() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.register(request("  " + EVENT_ID + "\t", EventType.PRODUCTION, null)))
                .isInstanceOfSatisfying(DuplicateEventException.class, this::assertDuplicate);

        assertNothingPersisted();
    }

    @Test
    @DisplayName("EV-13 sin timestamp se usa la hora del Clock truncada a segundos en la zona del sistema")
    void givenNoTimestamp_whenRegistering_thenServerAssignsClockTimeTruncatedToSeconds() {
        givenEventIdIsNew();

        var response = eventService.register(request(EVENT_ID, EventType.PRODUCTION, null));

        assertThat(response.timestamp()).isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        var saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getTimestamp()).isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
    }

    @Test
    @DisplayName("EV-21 violación de uk_events_event_id tras un pre-check falso → duplicado y sin auditoría")
    void givenConcurrentInsertOfSameEventId_whenUniqueConstraintFails_thenDuplicate() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventRepository.saveAndFlush(any(Event.class)))
                .thenThrow(integrityViolation(new ConstraintViolationException(
                        "unique", new SQLException("23505"), REAL_H2_EVENT_ID_CONSTRAINT)));

        assertThatThrownBy(() -> eventService.register(request(EVENT_ID, EventType.QUALITY, TIMESTAMP)))
                .isInstanceOfSatisfying(DuplicateEventException.class, this::assertDuplicate);

        verifyNoInteractions(auditRecordRepository);
        assertThat(meterRegistry.counter(EventService.DUPLICATES_METRIC).count()).isEqualTo(1.0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherIntegrityViolations")
    @DisplayName("EV-22 otra DataIntegrityViolationException se propaga sin traducirse a duplicado")
    void givenOtherIntegrityViolation_whenRegistering_thenItPropagates(
            String scenario, DataIntegrityViolationException violation) {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventRepository.saveAndFlush(any(Event.class))).thenThrow(violation);

        assertThatThrownBy(() -> eventService.register(request(EVENT_ID, EventType.QUALITY, TIMESTAMP)))
                .isSameAs(violation);

        verifyNoInteractions(auditRecordRepository);
        assertThat(meterRegistry.counter(EventService.DUPLICATES_METRIC).count()).isZero();
    }

    @Test
    @DisplayName("EV-24 métricas: events.registered por tipo en éxito y events.duplicates en duplicado")
    void givenSuccessesAndDuplicate_whenRegistering_thenCountersReflectEachOutcome() {
        when(eventRepository.existsByEventId("EVT-P")).thenReturn(false);
        when(eventRepository.existsByEventId("EVT-Q")).thenReturn(false, true);
        when(eventRepository.saveAndFlush(any(Event.class))).then(returnsFirstArg());
        when(auditRecordRepository.saveAndFlush(any(AuditRecord.class))).then(returnsFirstArg());

        eventService.register(request("EVT-P", EventType.PRODUCTION, TIMESTAMP));
        eventService.register(request("EVT-Q", EventType.QUALITY, TIMESTAMP));
        assertThatThrownBy(() -> eventService.register(request("EVT-Q", EventType.QUALITY, TIMESTAMP)))
                .isInstanceOf(DuplicateEventException.class);

        assertThat(registeredCount(EventType.PRODUCTION)).isEqualTo(1.0);
        assertThat(registeredCount(EventType.QUALITY)).isEqualTo(1.0);
        assertThat(meterRegistry.counter(EventService.DUPLICATES_METRIC).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("GT-01 eventId existente → devuelve el evento")
    void givenExistingEventId_whenFinding_thenReturnsEvent() {
        when(eventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(new Event(EVENT_ID, EventType.QUALITY, "Check", TIMESTAMP, CLOCK.instant())));

        assertThat(eventService.findByEventId(EVENT_ID))
                .isEqualTo(new EventResponse(EVENT_ID, EventType.QUALITY, "Check", TIMESTAMP, CLOCK.instant()));
    }

    @Test
    @DisplayName("GT-01 eventId inexistente → EVENT_NOT_FOUND")
    void givenUnknownEventId_whenFinding_thenEventNotFound() {
        when(eventRepository.findByEventId("EVT-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByEventId("EVT-404"))
                .isInstanceOfSatisfying(EventNotFoundException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND);
                    assertThat(error.getMessage()).isEqualTo("No existe un evento con eventId 'EVT-404'.");
                });
    }

    @Test
    @DisplayName("LS-03 sin page ni size se consulta la página 0 de 20, por receivedAt e id descendentes")
    void givenNoPaginationParameters_whenListing_thenDefaultsAndNewestFirstOrder() {
        var stored = new Event(EVENT_ID, EventType.PRODUCTION, "Line started", TIMESTAMP, CLOCK.instant());
        var expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "receivedAt", "id"));
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stored), expectedPageable, 1));

        var page = eventService.findPage(new PageQuery(null, null));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(expectedPageable);
        assertThat(page.content()).extracting(EventResponse::eventId).containsExactly(EVENT_ID);
        assertThat(page).extracting("page", "size", "totalElements", "totalPages").containsExactly(0, 20, 1L, 1);
    }

    static Stream<Arguments> otherIntegrityViolations() {
        return Stream.of(
                Arguments.of("constraint de auditoría", integrityViolation(new ConstraintViolationException(
                        "unique", new SQLException("23505"),
                        "PUBLIC.UK_AUDIT_RECORDS_EVENT_ID INDEX PUBLIC.UK_AUDIT_RECORDS_EVENT_ID_INDEX_F"))),
                Arguments.of("constraint sin nombre", integrityViolation(new ConstraintViolationException(
                        "not null", new SQLException("23502"), null))),
                Arguments.of("causa que no es de constraint", integrityViolation(new IllegalStateException("x"))));
    }

    private void givenEventIdIsNew() {
        when(eventRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventRepository.saveAndFlush(any(Event.class))).then(returnsFirstArg());
    }

    private void assertDuplicate(DuplicateEventException error) {
        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EVENT);
        assertThat(error.getMessage()).isEqualTo(DUPLICATE_MESSAGE);
    }

    private void assertNothingPersisted() {
        verify(eventRepository, never()).saveAndFlush(any(Event.class));
        verifyNoInteractions(auditRecordRepository);
        assertThat(meterRegistry.counter(EventService.DUPLICATES_METRIC).count()).isEqualTo(1.0);
        assertThat(meterRegistry.find(EventService.REGISTERED_METRIC).counters()).isEmpty();
    }

    private double registeredCount(EventType type) {
        return meterRegistry.counter(EventService.REGISTERED_METRIC, "type", type.name()).count();
    }

    private static DataIntegrityViolationException integrityViolation(Throwable cause) {
        return new DataIntegrityViolationException("integrity violation", cause);
    }

    private static EventRequest request(String eventId, EventType type, LocalDateTime timestamp) {
        return new EventRequest(eventId, type, "Line started", timestamp);
    }
}
