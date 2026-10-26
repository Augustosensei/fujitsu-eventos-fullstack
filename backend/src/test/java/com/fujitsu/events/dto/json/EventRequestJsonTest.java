package com.fujitsu.events.dto.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.model.EventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

class EventRequestJsonTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "2026-05-20T10:00       | 2026-05-20T10:00:00",
                "2026-05-20T10:00:00    | 2026-05-20T10:00:00",
                "2026-05-20T10:00:00.5  | 2026-05-20T10:00:00.500"
            })
    @DisplayName("EV-14 timestamps ISO-8601 sin zona se aceptan tal cual")
    void givenIsoLocalTimestamp_whenDeserializing_thenItIsParsed(String raw, LocalDateTime expected) {
        var request = read(body("\"EVT-1\"", "\"QUALITY\"", "\"Check\"", quoted(raw)));

        assertThat(request)
                .isEqualTo(new EventRequest("EVT-1", EventType.QUALITY, "Check", expected));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "2026-05-20T10:00:00Z",
                "2026-05-20T10:00:00-03:00",
                "2026-05-20T10:00:00[America/Argentina/Buenos_Aires]",
                "2026-02-30T10:00:00",
                "20/05/2026"
            })
    @DisplayName("EV-15 timestamps con zona, inexistentes o de otro formato se rechazan")
    void givenInvalidTimestampText_whenDeserializing_thenInvalidFormatOnTimestamp(String raw) {
        var json = body("\"EVT-1\"", "\"QUALITY\"", "\"Check\"", quoted(raw));

        assertThatThrownBy(() -> read(json))
                .isInstanceOfSatisfying(
                        InvalidFormatException.class, error -> assertField(error, "timestamp"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"\"\"", "\"   \"", "123", "true", "[2026,5,20,10,0]", "{}"})
    @DisplayName("EV-26 timestamp vacío, en blanco o que no es string JSON se rechaza")
    void givenNonStringTimestamp_whenDeserializing_thenMismatchedInputOnTimestamp(String raw) {
        var json = body("\"EVT-1\"", "\"QUALITY\"", "\"Check\"", raw);

        assertThatThrownBy(() -> read(json))
                .isInstanceOfSatisfying(
                        MismatchedInputException.class, error -> assertField(error, "timestamp"));
    }

    @Test
    @DisplayName("EV-13 timestamp ausente o null queda null para que lo asigne el servidor")
    void givenMissingOrNullTimestamp_whenDeserializing_thenTimestampIsNull() {
        var absent = read("""
                {"eventId":"EVT-1","eventType":"PRODUCTION","description":"Start"}""");
        var explicitNull = read(body("\"EVT-1\"", "\"PRODUCTION\"", "\"Start\"", "null"));

        assertThat(absent.timestamp()).isNull();
        assertThat(explicitNull.timestamp()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"OTHER", "quality", "", " QUALITY"})
    @DisplayName("EV-11 eventType fuera de los valores exactos se rechaza indicando los permitidos")
    void givenUnknownEventType_whenDeserializing_thenInvalidFormatListsAllowedValues(String raw) {
        var json = body("\"EVT-1\"", quoted(raw), "\"Check\"", "null");

        assertThatThrownBy(() -> read(json))
                .isInstanceOfSatisfying(
                        InvalidFormatException.class,
                        error -> {
                            assertField(error, "eventType");
                            assertThat(error.getMessage()).contains("[PRODUCTION, QUALITY]");
                        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"0", "1", "true", "[\"QUALITY\"]", "{}"})
    @DisplayName("EV-25 eventType que no es string JSON se rechaza (sin mapear por posición)")
    void givenNonStringEventType_whenDeserializing_thenMismatchedInputOnEventType(String raw) {
        var json = body("\"EVT-1\"", raw, "\"Check\"", "null");

        assertThatThrownBy(() -> read(json))
                .isInstanceOfSatisfying(
                        MismatchedInputException.class, error -> assertField(error, "eventType"));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "eventId     | 123",
                "eventId     | true",
                "eventId     | {}",
                "eventId     | [\"EVT-1\"]",
                "description | 123",
                "description | false",
                "description | {}",
                "description | [\"Check\"]"
            })
    @DisplayName("EV-27 eventId y description que no son string JSON se rechazan")
    void givenNonStringTextField_whenDeserializing_thenMismatchedInputOnThatField(
            String field, String raw) {
        var eventId = field.equals("eventId") ? raw : "\"EVT-1\"";
        var description = field.equals("description") ? raw : "\"Check\"";
        var json = body(eventId, "\"PRODUCTION\"", description, "null");

        assertThatThrownBy(() -> read(json))
                .isInstanceOfSatisfying(MismatchedInputException.class, error -> assertField(error, field));
    }

    private EventRequest read(String json) {
        return jsonMapper.readValue(json, EventRequest.class);
    }

    private static String body(String eventId, String eventType, String description, String timestamp) {
        return """
                {"eventId":%s,"eventType":%s,"description":%s,"timestamp":%s}"""
                .formatted(eventId, eventType, description, timestamp);
    }

    private static String quoted(String value) {
        return "\"" + value + "\"";
    }

    private static void assertField(MismatchedInputException error, String field) {
        assertThat(error.getPath()).last().extracting(reference -> reference.getPropertyName()).isEqualTo(field);
    }
}
