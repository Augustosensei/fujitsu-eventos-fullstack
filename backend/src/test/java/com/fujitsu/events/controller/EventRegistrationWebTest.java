package com.fujitsu.events.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.exception.DuplicateEventException;
import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class EventRegistrationWebTest extends WebLayerTestSupport {

    private static final Instant RECEIVED_AT = Instant.parse("2026-05-20T13:00:00.789Z");

    EventRegistrationWebTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @Test
    @DisplayName("EV-01 PRODUCTION válido → 201 con Location y cuerpo con receivedAt")
    void givenValidProductionEvent_whenPosting_thenCreatedWithLocationAndBody() throws Exception {
        givenServiceEchoesRequest();

        postEvent(eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, VALID_TIMESTAMP))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/EVT-1001"))
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.eventType").value("PRODUCTION"))
                .andExpect(jsonPath("$.description").value("Production line started"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-20T10:00:00"))
                .andExpect(jsonPath("$.receivedAt").value("2026-05-20T13:00:00.789Z"));

        verify(eventService)
                .register(new EventRequest(
                        "EVT-1001", EventType.PRODUCTION, "Production line started", LocalDateTime.of(2026, 5, 20, 10, 0)));
    }

    @Test
    @DisplayName("EV-03 eventId ya registrado → 409 DUPLICATE_EVENT con el mensaje del dominio")
    void givenDuplicatedEventId_whenPosting_thenConflictProblemDetail() throws Exception {
        given(eventService.register(any())).willThrow(new DuplicateEventException("EVT-1001"));

        postEvent(eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, null))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("json")))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Evento duplicado"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_EVENT"))
                .andExpect(jsonPath("$.detail")
                        .value("El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @ParameterizedTest(name = "eventId = {0}")
    @ValueSource(strings = {"<ausente>", "null", "\"\"", "\"   \""})
    @DisplayName("EV-08 eventId ausente, null, vacío o solo espacios → 400 VALIDATION_ERROR en eventId")
    void givenMissingOrBlankEventId_whenPosting_thenValidationErrorOnEventId(String eventId) throws Exception {
        var rawEventId = "<ausente>".equals(eventId) ? null : eventId;

        expectSingleFieldError(eventJson(rawEventId, VALID_TYPE, VALID_DESCRIPTION, null), "eventId")
                .andExpect(jsonPath("$.errors[0].message").value("El eventId es obligatorio."));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("EV-09 eventId de 64 caracteres → 201; de 65 → 400 con el máximo en el mensaje")
    void givenEventIdAtAndOverMaxLength_whenPosting_thenAcceptedAndRejected() throws Exception {
        givenServiceEchoesRequest();

        postEvent(eventJson(quoted("A".repeat(64)), VALID_TYPE, VALID_DESCRIPTION, null))
                .andExpect(status().isCreated());
        expectSingleFieldError(eventJson(quoted("A".repeat(65)), VALID_TYPE, VALID_DESCRIPTION, null), "eventId")
                .andExpect(jsonPath("$.errors[0].message").value("El eventId puede tener como máximo 64 caracteres."));
    }

    @ParameterizedTest(name = "eventId = {0}")
    @ValueSource(strings = {"EVT 1001", "EVT#1001", "EVTá1001"})
    @DisplayName("EV-10 eventId con espacio interno, # o acento → 400 por el patrón")
    void givenEventIdOutsidePattern_whenPosting_thenValidationError(String eventId) throws Exception {
        expectSingleFieldError(eventJson(quoted(eventId), VALID_TYPE, VALID_DESCRIPTION, null), "eventId")
                .andExpect(jsonPath("$.errors[0].message", containsString("solo admite letras sin acentos")));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("EV-11 eventType ausente → 400 VALIDATION_ERROR en eventType")
    void givenMissingEventType_whenPosting_thenValidationErrorOnEventType() throws Exception {
        expectSingleFieldError(eventJson(VALID_EVENT_ID, null, VALID_DESCRIPTION, null), "eventType")
                .andExpect(jsonPath("$.errors[0].message").value("El tipo de evento es obligatorio."));
    }

    @ParameterizedTest(name = "eventType = {0}")
    @ValueSource(strings = {"\"OTHER\"", "\"quality\""})
    @DisplayName("EV-11 eventType desconocido o en minúsculas → 400 indicando los valores permitidos")
    void givenUnknownEventType_whenPosting_thenValidationErrorListingAllowedValues(String eventType) throws Exception {
        expectSingleFieldError(eventJson(VALID_EVENT_ID, eventType, VALID_DESCRIPTION, null), "eventType")
                .andExpect(jsonPath("$.errors[0].message")
                        .value("El valor de 'eventType' no es válido. Valores permitidos: PRODUCTION, QUALITY."));
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "description = {0}")
    @ValueSource(strings = {"<ausente>", "\"\"", "\"   \""})
    @DisplayName("EV-12 description ausente, vacía o en blanco → 400 VALIDATION_ERROR en description")
    void givenMissingOrBlankDescription_whenPosting_thenValidationError(String description) throws Exception {
        var rawDescription = "<ausente>".equals(description) ? null : description;

        expectSingleFieldError(eventJson(VALID_EVENT_ID, VALID_TYPE, rawDescription, null), "description")
                .andExpect(jsonPath("$.errors[0].message").value("La descripción es obligatoria."));
    }

    @Test
    @DisplayName("EV-12 description de 500 caracteres → 201; de 501 → 400")
    void givenDescriptionAtAndOverMaxLength_whenPosting_thenAcceptedAndRejected() throws Exception {
        givenServiceEchoesRequest();

        postEvent(eventJson(VALID_EVENT_ID, VALID_TYPE, quoted("d".repeat(500)), null))
                .andExpect(status().isCreated());
        expectSingleFieldError(eventJson(VALID_EVENT_ID, VALID_TYPE, quoted("d".repeat(501)), null), "description")
                .andExpect(jsonPath("$.errors[0].message")
                        .value("La descripción puede tener como máximo 500 caracteres."));
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @MethodSource("acceptedTimestamps")
    @DisplayName("EV-14 timestamp ISO sin zona con o sin segundos y fracción → 201 y llega al servicio igual")
    void givenIsoLocalTimestamp_whenPosting_thenServiceReceivesSameValue(String timestamp, LocalDateTime expected)
            throws Exception {
        givenServiceEchoesRequest();

        postEvent(eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, quoted(timestamp)))
                .andExpect(status().isCreated());

        var request = ArgumentCaptor.forClass(EventRequest.class);
        verify(eventService).register(request.capture());
        assertThat(request.getValue().timestamp()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @ValueSource(strings = {"2026-05-20T10:00:00Z", "2026-05-20T10:00:00-03:00", "2026-02-30T10:00:00", "20/05/2026"})
    @DisplayName("EV-15 timestamp con zona, fecha inexistente u otro formato → 400 VALIDATION_ERROR en timestamp")
    void givenInvalidTimestamp_whenPosting_thenValidationErrorOnTimestamp(String timestamp) throws Exception {
        expectSingleFieldError(eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, quoted(timestamp)), "timestamp")
                .andExpect(jsonPath("$.errors[0].message", containsString("ISO-8601 sin zona horaria")));
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "body = [{0}]")
    @ValueSource(strings = {"", "{", "{\"eventId\": \"EVT-1001\",}", "null", "[]", "\"texto\""})
    @DisplayName("EV-16 body vacío, JSON roto o raíz que no es objeto → 400 MALFORMED_REQUEST")
    void givenMalformedBody_whenPosting_thenMalformedRequest(String body) throws Exception {
        postEvent(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.title").value("Solicitud mal formada"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("EV-19 propiedad JSON desconocida → se ignora (comportamiento por defecto de Boot, DT10)")
    void givenUnknownJsonProperty_whenPosting_thenIgnoredAndCreated() throws Exception {
        givenServiceEchoesRequest();
        var body = """
                {"eventId": "EVT-1001", "eventType": "PRODUCTION", "description": "Production line started", \
                "priority": "HIGH"}""";

        postEvent(body).andExpect(status().isCreated());

        verify(eventService)
                .register(new EventRequest("EVT-1001", EventType.PRODUCTION, "Production line started", null));
    }

    @ParameterizedTest(name = "eventType = {0}")
    @ValueSource(strings = {"\"\"", "0", "1"})
    @DisplayName("EV-25 eventType vacío o numérico → 400 en eventType, sin mapear por posición del enum")
    void givenEmptyOrNumericEventType_whenPosting_thenValidationErrorOnEventType(String eventType) throws Exception {
        expectSingleFieldError(eventJson(VALID_EVENT_ID, eventType, VALID_DESCRIPTION, null), "eventType")
                .andExpect(jsonPath("$.errors[0].message", containsString("PRODUCTION, QUALITY")));
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "timestamp = {0}")
    @ValueSource(strings = {"\"\"", "\"   \"", "1747735200", "[2026,5,20,10,0]"})
    @DisplayName("EV-26 timestamp vacío, en blanco, numérico o arreglo → 400 en timestamp")
    void givenNonIsoStringTimestamp_whenPosting_thenValidationErrorOnTimestamp(String timestamp) throws Exception {
        expectSingleFieldError(eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, timestamp), "timestamp");
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("nonStringTextValues")
    @DisplayName("EV-27 eventId o description como número, booleano, objeto o arreglo → 400 en ese campo")
    void givenNonStringTextField_whenPosting_thenValidationErrorOnThatField(String field, String rawValue)
            throws Exception {
        var body = "eventId".equals(field)
                ? eventJson(rawValue, VALID_TYPE, VALID_DESCRIPTION, null)
                : eventJson(VALID_EVENT_ID, VALID_TYPE, rawValue, null);

        expectSingleFieldError(body, field)
                .andExpect(jsonPath("$.errors[0].message").value("'%s' debe ser un texto (string JSON).".formatted(field)));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("DT9 varios campos inválidos → todos en errors, ordenados por campo")
    void givenSeveralInvalidFields_whenPosting_thenAllErrorsSortedByField() throws Exception {
        postEvent("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("La solicitud tiene datos inválidos. Revisá los campos indicados."))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[1].field").value("eventId"))
                .andExpect(jsonPath("$.errors[2].field").value("eventType"));
    }

    static Stream<Arguments> acceptedTimestamps() {
        return Stream.of(
                Arguments.of("2026-05-20T10:00", LocalDateTime.of(2026, 5, 20, 10, 0)),
                Arguments.of("2026-05-20T10:00:00", LocalDateTime.of(2026, 5, 20, 10, 0, 0)),
                Arguments.of("2026-05-20T10:00:00.5", LocalDateTime.of(2026, 5, 20, 10, 0, 0, 500_000_000)));
    }

    static Stream<Arguments> nonStringTextValues() {
        return Stream.of("eventId", "description")
                .flatMap(field -> Stream.of("1001", "true", "{\"value\": \"x\"}", "[\"x\"]")
                        .map(value -> Arguments.of(field, value)));
    }

    private void givenServiceEchoesRequest() {
        given(eventService.register(any())).willAnswer(invocation -> {
            EventRequest request = invocation.getArgument(0);
            return new EventResponse(
                    request.eventId(), request.eventType(), request.description(), request.timestamp(), RECEIVED_AT);
        });
    }

    private ResultActions postEvent(String body) throws Exception {
        return mockMvc.perform(
                post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions expectSingleFieldError(String body, String field) throws Exception {
        return postEvent(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(field));
    }
}
