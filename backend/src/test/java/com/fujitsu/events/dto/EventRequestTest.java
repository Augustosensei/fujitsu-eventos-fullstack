package com.fujitsu.events.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fujitsu.events.model.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EventRequestTest {

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"  EVT-1001  ", "\tEVT-1001\n", "EVT-1001"})
    @DisplayName("EV-06 el eventId se recorta con strip al construir el request")
    void givenEventIdWithSurroundingWhitespace_whenCreatingRequest_thenEventIdIsStripped(String raw) {
        var request = new EventRequest(raw, EventType.PRODUCTION, "Line started", null);

        assertThat(request.eventId()).isEqualTo("EVT-1001");
    }

    @Test
    @DisplayName("EV-08 un eventId ausente queda null para que lo rechace la validación")
    void givenNullEventId_whenCreatingRequest_thenEventIdStaysNull() {
        var request = new EventRequest(null, EventType.PRODUCTION, "Line started", null);

        assertThat(request.eventId()).isNull();
    }
}
