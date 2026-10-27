package com.fujitsu.events.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class PlatformEndpointsIntegrationTest extends IntegrationTestSupport {

    private final SpringDocConfigProperties springDocProperties;
    private final SwaggerUiConfigProperties swaggerUiProperties;

    PlatformEndpointsIntegrationTest(
            MockMvc mockMvc,
            EventRepository eventRepository,
            AuditRecordRepository auditRecordRepository,
            SpringDocConfigProperties springDocProperties,
            SwaggerUiConfigProperties swaggerUiProperties) {
        super(mockMvc, eventRepository, auditRecordRepository);
        this.springDocProperties = springDocProperties;
        this.swaggerUiProperties = swaggerUiProperties;
    }

    @Test
    @DisplayName("OB-04 /v3/api-docs → 200 con título, versión del proyecto y las respuestas documentadas")
    void givenRunningApp_whenRequestingOpenApi_thenDocumentedContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de eventos de fábrica"))
                .andExpect(jsonPath("$.info.version").value("0.0.1-SNAPSHOT"))
                .andExpect(jsonPath("$.paths['/api/events'].post.responses", hasKey("201")))
                .andExpect(jsonPath("$.paths['/api/events'].post.responses", hasKey("409")))
                .andExpect(jsonPath("$.paths['/api/events'].post.responses", hasKey("415")))
                .andExpect(jsonPath("$.paths['/api/events/{eventId}'].get.responses", hasKey("404")))
                .andExpect(jsonPath("$.paths['/api/audits'].get.responses", hasKey("400")));
    }

    @Test
    @DisplayName("OB-04 /actuator/health → 200 UP")
    void givenRunningApp_whenRequestingHealth_thenUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("OB-06 springdoc habilitado de forma explícita: arranque sin WARN de springdoc y Swagger UI disponible")
    void givenDefaultConfiguration_whenContextStarts_thenSpringdocEndpointsAreExplicitlyEnabled() throws Exception {
        assertThat(springDocProperties.getApiDocs().isEnabled()).isTrue();
        assertThat(swaggerUiProperties.isEnabled()).isTrue();
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("OB-07 error fuera de los controllers (/error) → 500 sin stack trace aunque se pida trace=true")
    void givenUnhandledServletError_whenRequestingTrace_thenStackTraceIsNeverExposed() throws Exception {
        mockMvc.perform(get("/error")
                        .param("trace", "true")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("boom")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
