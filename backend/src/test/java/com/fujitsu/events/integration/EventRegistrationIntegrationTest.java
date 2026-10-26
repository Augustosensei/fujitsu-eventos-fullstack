package com.fujitsu.events.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.model.EventType;
import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

class EventRegistrationIntegrationTest extends IntegrationTestSupport {

    private static final String DUPLICATES_METRIC = "events.duplicates";

    private final MeterRegistry meterRegistry;

    EventRegistrationIntegrationTest(
            MockMvc mockMvc,
            EventRepository eventRepository,
            AuditRecordRepository auditRecordRepository,
            MeterRegistry meterRegistry) {
        super(mockMvc, eventRepository, auditRecordRepository);
        this.meterRegistry = meterRegistry;
    }

    @Test
    @DisplayName("EV-01 PRODUCTION válido → 201 con Location y receivedAt del servidor; 1 evento, 0 auditorías")
    void givenValidProduction_whenPosting_thenStoredWithoutAudit() throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/EVT-1001"))
                .andExpect(jsonPath("$.receivedAt").value("2026-05-20T13:00:00.789Z"));

        assertThat(eventRepository.findByEventId("EVT-1001"))
                .hasValueSatisfying(event -> assertThat(event.getEventType()).isEqualTo(EventType.PRODUCTION));
        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("EV-02 QUALITY válido → 201; 1 evento y 1 auditoría con ese eventId visible en /api/audits")
    void givenValidQuality_whenPosting_thenStoredWithAudit() throws Exception {
        postEvent(eventJson("EVT-2001", "QUALITY", "Quality check")).andExpect(status().isCreated());

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/api/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventId").value("EVT-2001"))
                .andExpect(jsonPath("$.content[0].auditedAt").value("2026-05-20T13:00:00.789Z"));
    }

    @Test
    @DisplayName("EV-03 reenvío del mismo eventId PRODUCTION → 409 DUPLICATE_EVENT; conteos sin cambio")
    void givenRegisteredProduction_whenResending_thenConflictAndNoNewRows() throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EVENT"));

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("EV-04 reenvío del mismo eventId QUALITY → 409; las auditorías siguen en 1")
    void givenRegisteredQuality_whenResending_thenConflictAndSingleAudit() throws Exception {
        postEvent(eventJson("EVT-2001", "QUALITY", "Quality check")).andExpect(status().isCreated());

        postEvent(eventJson("EVT-2001", "QUALITY", "Quality check")).andExpect(status().isConflict());

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("EV-05 mismo eventId con otro tipo y descripción (PRODUCTION y luego QUALITY) → 409; 0 auditorías")
    void givenRegisteredProduction_whenResendingAsQuality_thenConflictAndNoAudit() throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        postEvent(eventJson("EVT-1001", "QUALITY", "Otro texto")).andExpect(status().isConflict());

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isZero();
    }

    @ParameterizedTest(name = "eventId = {0}")
    @ValueSource(strings = {"  EVT-1001  ", "\\tEVT-1001", "EVT-1001\\n"})
    @DisplayName("EV-06 eventId con espacios, tab o salto de línea alrededor de uno existente → 409")
    void givenRegisteredEventId_whenResendingWithSurroundingWhitespace_thenConflict(String eventId)
            throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        postEvent(eventJson(eventId, "PRODUCTION", "Production line started"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo."));

        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("EV-07 eventId que difiere solo en mayúsculas → 201 (son eventos distintos)")
    void givenRegisteredEventId_whenPostingDifferentCase_thenCreated() throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        postEvent(eventJson("evt-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        assertThat(eventRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("EV-09 eventId de 64 caracteres → 201 y persistido; de 65 → 400 sin persistir")
    void givenEventIdAtAndOverMaxLength_whenPosting_thenOnlyValidPersisted() throws Exception {
        var maxLength = "A".repeat(64);

        postEvent(eventJson(maxLength, "PRODUCTION", "x")).andExpect(status().isCreated());
        postEvent(eventJson("B".repeat(65), "PRODUCTION", "x")).andExpect(status().isBadRequest());

        assertThat(eventRepository.findByEventId(maxLength)).isPresent();
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("EV-12 description de 500 caracteres → 201 y persistida completa; de 501 → 400 sin persistir")
    void givenDescriptionAtAndOverMaxLength_whenPosting_thenOnlyValidPersisted() throws Exception {
        var maxDescription = "d".repeat(500);

        postEvent(eventJson("EVT-500", "PRODUCTION", maxDescription)).andExpect(status().isCreated());
        postEvent(eventJson("EVT-501", "PRODUCTION", "d".repeat(501))).andExpect(status().isBadRequest());

        assertThat(eventRepository.findByEventId("EVT-500"))
                .hasValueSatisfying(event -> assertThat(event.getDescription()).isEqualTo(maxDescription));
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @ValueSource(strings = {"<ausente>", "null"})
    @DisplayName("EV-13 timestamp ausente o null → lo asigna el servidor con el Clock truncado a segundos")
    void givenNoTimestamp_whenPosting_thenServerAssignsClockTruncatedToSeconds(String timestamp) throws Exception {
        var body = "<ausente>".equals(timestamp)
                ? eventJson("EVT-1001", "PRODUCTION", "x")
                : """
                {"eventId": "EVT-1001", "eventType": "PRODUCTION", "description": "x", "timestamp": null}""";

        postEvent(body).andExpect(status().isCreated()).andExpect(jsonPath("$.timestamp").value("2026-05-20T13:00:00"));

        assertThat(eventRepository.findByEventId("EVT-1001"))
                .hasValueSatisfying(event ->
                        assertThat(event.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 5, 20, 13, 0, 0)));
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @CsvSource({
        "2026-05-20T10:00, 2026-05-20T10:00:00",
        "2026-05-20T10:00:00, 2026-05-20T10:00:00",
        "2026-05-20T10:00:00.5, 2026-05-20T10:00:00.5"
    })
    @DisplayName("EV-14 timestamp ISO sin zona con o sin segundos y fracción → 201, persistido y devuelto igual")
    void givenIsoLocalTimestamp_whenPostingAndGetting_thenSameValue(String sent, String returned) throws Exception {
        assertTimestampRoundTrip(sent, returned);
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @ValueSource(strings = {"2099-12-31T23:59:59", "1900-01-01T00:00:00"})
    @DisplayName("EV-29 timestamp futuro o antiguo → 201 y devuelto igual (D5 solo valida el formato)")
    void givenFutureOrAncientTimestamp_whenPosting_thenCreatedUnchanged(String timestamp) throws Exception {
        assertTimestampRoundTrip(timestamp, timestamp);
    }

    @Test
    @DisplayName("EV-28 eventId ya registrado + otro campo inválido → 400 VALIDATION_ERROR; conteos y duplicados sin cambio")
    void givenRegisteredEventIdAndInvalidField_whenPosting_thenValidationRunsBeforeDuplicateCheck() throws Exception {
        postEvent(eventJson("EVT-1001", "QUALITY", "Quality check")).andExpect(status().isCreated());
        var duplicatesBefore = meterRegistry.counter(DUPLICATES_METRIC).count();

        postEvent(eventJson("EVT-1001", "QUALITY", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("description"));

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isEqualTo(1);
        assertThat(meterRegistry.counter(DUPLICATES_METRIC).count()).isEqualTo(duplicatesBefore);
    }

    @Test
    @DisplayName("EV-30 description con espacios alrededor y salto de línea interno → 201 y se guarda sin recorte")
    void givenDescriptionWithWhitespace_whenPosting_thenStoredUntouched() throws Exception {
        var description = "  línea 1\nlínea 2  ";

        postEvent(eventJson("EVT-1001", "PRODUCTION", "  línea 1\\nlínea 2  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(description));

        assertThat(eventRepository.findByEventId("EVT-1001"))
                .hasValueSatisfying(event -> assertThat(event.getDescription()).isEqualTo(description));
    }

    private void assertTimestampRoundTrip(String sent, String returned) throws Exception {
        var body = """
                {"eventId": "EVT-TS", "eventType": "PRODUCTION", "description": "x", "timestamp": "%s"}"""
                .formatted(sent);

        postEvent(body).andExpect(status().isCreated()).andExpect(jsonPath("$.timestamp").value(returned));

        mockMvc.perform(get("/api/events/{eventId}", "EVT-TS")).andExpect(jsonPath("$.timestamp").value(returned));
        assertThat(eventRepository.findByEventId("EVT-TS"))
                .hasValueSatisfying(event -> assertThat(event.getTimestamp()).isEqualTo(LocalDateTime.parse(sent)));
    }
}
