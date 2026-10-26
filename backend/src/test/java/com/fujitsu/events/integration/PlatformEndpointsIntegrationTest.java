package com.fujitsu.events.integration;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class PlatformEndpointsIntegrationTest extends IntegrationTestSupport {

    PlatformEndpointsIntegrationTest(
            MockMvc mockMvc, EventRepository eventRepository, AuditRecordRepository auditRecordRepository) {
        super(mockMvc, eventRepository, auditRecordRepository);
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
}
