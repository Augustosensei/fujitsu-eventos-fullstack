package com.fujitsu.events.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class QueryIntegrationTest extends IntegrationTestSupport {

    QueryIntegrationTest(
            MockMvc mockMvc, EventRepository eventRepository, AuditRecordRepository auditRecordRepository) {
        super(mockMvc, eventRepository, auditRecordRepository);
    }

    @Test
    @DisplayName("LS-01 sin eventos en la base → 200 con content vacío, totalElements 0 y defaults page 0 / size 20")
    void givenEmptyDatabase_whenListingEvents_thenEmptyPageWithDefaults() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("GT-05 GET /api/events/%20EVT-1001%20 → 200 con el evento EVT-1001")
    void givenRegisteredEvent_whenGettingWithSurroundingSpaces_thenFound() throws Exception {
        postEvent(eventJson("EVT-1001", "PRODUCTION", "Production line started")).andExpect(status().isCreated());

        mockMvc.perform(get(URI.create("/api/events/%20EVT-1001%20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.description").value("Production line started"));
    }

    @Test
    @DisplayName("AU-01 solo eventos PRODUCTION registrados → /api/audits con content vacío")
    void givenOnlyProductionEvents_whenListingAudits_thenEmpty() throws Exception {
        postEvent(eventJson("EVT-1", "PRODUCTION", "a")).andExpect(status().isCreated());
        postEvent(eventJson("EVT-2", "PRODUCTION", "b")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/events")).andExpect(jsonPath("$.totalElements").value(2));
    }
}
