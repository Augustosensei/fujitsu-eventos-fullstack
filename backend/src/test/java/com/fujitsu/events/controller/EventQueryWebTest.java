package com.fujitsu.events.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import com.fujitsu.events.exception.EventNotFoundException;
import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

class EventQueryWebTest extends WebLayerTestSupport {

    private static final EventResponse EVENT = new EventResponse(
            "EVT-1001",
            EventType.QUALITY,
            "Production line started",
            LocalDateTime.of(2026, 5, 20, 10, 0),
            Instant.parse("2026-05-20T13:00:00Z"));

    EventQueryWebTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @Test
    @DisplayName("LS-01 sin eventos → 200 con content vacío y totalElements 0")
    void givenNoEvents_whenListing_thenEmptyPage() throws Exception {
        given(eventService.findPage(any())).willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("LS-03 sin page ni size → el servicio recibe ambos nulos para aplicar los defaults")
    void givenNoPaginationParams_whenListing_thenServiceAppliesDefaults() throws Exception {
        given(eventService.findPage(any())).willReturn(new PageResponse<>(List.of(EVENT), 0, 20, 1, 1));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(eventService).findPage(new PageQuery(null, null));
    }

    @Test
    @DisplayName("LS-04 size igual al máximo configurado (100) → 200")
    void givenSizeAtMax_whenListing_thenOk() throws Exception {
        given(eventService.findPage(any())).willReturn(new PageResponse<>(List.of(), 2, 100, 0, 0));

        mockMvc.perform(get("/api/events").param("page", "2").param("size", "100")).andExpect(status().isOk());

        verify(eventService).findPage(new PageQuery(2, 100));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "size, 101, El tamaño de página puede ser como máximo 100.",
        "size, 0, El tamaño de página debe ser 1 o mayor.",
        "page, -1, El número de página debe ser 0 o mayor."
    })
    @DisplayName("LS-04 size máx+1 o 0, page -1 → 400 INVALID_PAGINATION con el campo y el motivo")
    void givenOutOfRangePagination_whenListing_thenInvalidPagination(String param, String value, String message)
            throws Exception {
        mockMvc.perform(get("/api/events").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.title").value("Paginación inválida"))
                .andExpect(jsonPath("$.detail").value("Los parámetros de paginación no son válidos."))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(param))
                .andExpect(jsonPath("$.errors[0].message").value(message));
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({"page, abc", "size, x", "page, 2147483648", "size, 1.5"})
    @DisplayName("LS-05 page o size no numéricos o fuera de int → 400 INVALID_PAGINATION")
    void givenNonIntegerPagination_whenListing_thenInvalidPagination(String param, String value) throws Exception {
        mockMvc.perform(get("/api/events").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(param))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("El parámetro '%s' debe ser un número entero válido.".formatted(param)));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("GT-01 eventId existente → 200 con el evento")
    void givenExistingEventId_whenGetting_thenOk() throws Exception {
        given(eventService.findByEventId("EVT-1001")).willReturn(EVENT);

        mockMvc.perform(get("/api/events/{eventId}", "EVT-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.eventType").value("QUALITY"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-20T10:00:00"))
                .andExpect(jsonPath("$.receivedAt").value("2026-05-20T13:00:00Z"));
    }

    @Test
    @DisplayName("GT-01 eventId inexistente → 404 EVENT_NOT_FOUND")
    void givenUnknownEventId_whenGetting_thenNotFound() throws Exception {
        given(eventService.findByEventId("EVT-404")).willThrow(new EventNotFoundException("EVT-404"));

        mockMvc.perform(get("/api/events/{eventId}", "EVT-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Evento no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe un evento con eventId 'EVT-404'."));
    }

    @ParameterizedTest(name = "eventId = [{0}]")
    @ValueSource(strings = {
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "EVT#1001",
        "   "
    })
    @DisplayName("GT-03 eventId del path de 65 caracteres, con # o solo espacios → 400 VALIDATION_ERROR")
    void givenPathEventIdOutsideRules_whenGetting_thenValidationError(String eventId) throws Exception {
        mockMvc.perform(get("/api/events/{eventId}", eventId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("eventId"));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("GT-05 eventId del path con espacios alrededor → se recorta y se busca EVT-1001")
    void givenPathEventIdWithSurroundingSpaces_whenGetting_thenStrippedAndFound() throws Exception {
        given(eventService.findByEventId("EVT-1001")).willReturn(EVENT);

        mockMvc.perform(get("/api/events/{eventId}", " EVT-1001 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-1001"));

        verify(eventService).findByEventId("EVT-1001");
    }
}
