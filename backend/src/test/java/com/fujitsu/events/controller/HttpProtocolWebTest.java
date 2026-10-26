package com.fujitsu.events.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.config.CorrelationIdFilter;
import com.fujitsu.events.dto.PageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class HttpProtocolWebTest extends WebLayerTestSupport {

    private static final String VALID_BODY =
            eventJson(VALID_EVENT_ID, VALID_TYPE, VALID_DESCRIPTION, VALID_TIMESTAMP);

    HttpProtocolWebTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @Test
    @DisplayName("EV-17 Content-Type text/plain → 415 UNSUPPORTED_MEDIA_TYPE")
    void givenTextPlainBody_whenPosting_thenUnsupportedMediaType() throws Exception {
        expectProblem(
                        mockMvc.perform(post("/api/events").contentType(MediaType.TEXT_PLAIN).content(VALID_BODY)),
                        415,
                        "UNSUPPORTED_MEDIA_TYPE")
                .andExpect(jsonPath("$.detail").value("El tipo de contenido no es compatible; usá application/json."));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("EV-18 PUT /api/events → 405 METHOD_NOT_ALLOWED con Allow")
    void givenPut_whenCallingEvents_thenMethodNotAllowed() throws Exception {
        expectProblem(
                        mockMvc.perform(put("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)),
                        405,
                        "METHOD_NOT_ALLOWED")
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("POST"))))
                .andExpect(jsonPath("$.detail").value("El método PUT no está permitido para este recurso."));
    }

    @Test
    @DisplayName("GT-02 ruta inexistente /api/nope → 404 RESOURCE_NOT_FOUND en ProblemDetail")
    void givenUnknownRoute_whenGetting_thenResourceNotFound() throws Exception {
        expectProblem(mockMvc.perform(get("/api/nope")), 404, "RESOURCE_NOT_FOUND")
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el recurso solicitado."));
    }

    @Test
    @DisplayName("OB-05 Accept application/xml en GET /api/events → 406 NOT_ACCEPTABLE en ProblemDetail")
    void givenXmlAccept_whenListing_thenNotAcceptable() throws Exception {
        expectProblem(mockMvc.perform(get("/api/events").accept(MediaType.APPLICATION_XML)), 406, "NOT_ACCEPTABLE")
                .andExpect(jsonPath("$.detail").value("La API solo puede responder en formato JSON."));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("OB-05 Accept application/xml en POST /api/events → 406 NOT_ACCEPTABLE y no registra")
    void givenXmlAccept_whenPosting_thenNotAcceptable() throws Exception {
        expectProblem(
                mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content(VALID_BODY)),
                406,
                "NOT_ACCEPTABLE");
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("OB-01 X-Correlation-Id válido → eco en el header de una respuesta exitosa")
    void givenValidCorrelationId_whenRequestSucceeds_thenEchoedInHeader() throws Exception {
        given(eventService.findPage(any())).willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/events").header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"));
    }

    @Test
    @DisplayName("OB-01 X-Correlation-Id válido → eco en el header y en ProblemDetail.correlationId")
    void givenValidCorrelationId_whenRequestFails_thenEchoedInHeaderAndProblem() throws Exception {
        mockMvc.perform(get("/api/nope").header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
