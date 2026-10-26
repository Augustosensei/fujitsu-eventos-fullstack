package com.fujitsu.events.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {

    static final Instant NOW = Instant.parse("2026-05-20T13:00:00.789Z");

    @TestBean
    Clock clock;

    final MockMvc mockMvc;
    final EventRepository eventRepository;
    final AuditRecordRepository auditRecordRepository;

    IntegrationTestSupport(
            MockMvc mockMvc, EventRepository eventRepository, AuditRecordRepository auditRecordRepository) {
        this.mockMvc = mockMvc;
        this.eventRepository = eventRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    static String eventJson(String eventId, String eventType, String description) {
        return """
                {"eventId": "%s", "eventType": "%s", "description": "%s"}"""
                .formatted(eventId, eventType, description);
    }

    @BeforeEach
    void cleanTables() {
        auditRecordRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    ResultActions postEvent(String body) throws Exception {
        return mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
