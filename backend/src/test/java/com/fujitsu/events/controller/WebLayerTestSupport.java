package com.fujitsu.events.controller;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.service.AuditService;
import com.fujitsu.events.service.EventService;
import java.util.StringJoiner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({EventController.class, AuditController.class})
@EnableConfigurationProperties(PaginationProperties.class)
abstract class WebLayerTestSupport {

    static final String VALID_EVENT_ID = "\"EVT-1001\"";
    static final String VALID_TYPE = "\"PRODUCTION\"";
    static final String VALID_DESCRIPTION = "\"Production line started\"";
    static final String VALID_TIMESTAMP = "\"2026-05-20T10:00:00\"";

    @MockitoBean
    EventService eventService;

    @MockitoBean
    AuditService auditService;

    final MockMvc mockMvc;

    WebLayerTestSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Builds an event payload from raw JSON values; a {@code null} argument omits the property. */
    static String eventJson(String eventId, String eventType, String description, String timestamp) {
        var json = new StringJoiner(",", "{", "}");
        addProperty(json, "eventId", eventId);
        addProperty(json, "eventType", eventType);
        addProperty(json, "description", description);
        addProperty(json, "timestamp", timestamp);
        return json.toString();
    }

    static String quoted(String value) {
        return "\"" + value + "\"";
    }

    private static void addProperty(StringJoiner json, String name, String rawValue) {
        if (rawValue != null) {
            json.add(quoted(name) + ":" + rawValue);
        }
    }
}
