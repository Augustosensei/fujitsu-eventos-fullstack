package com.fujitsu.events.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import com.fujitsu.events.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class AuditFailureIntegrationTest extends IntegrationTestSupport {

    private static final String INTERNAL_MESSAGE = "audit table unavailable: jdbc:h2:mem:events";

    @MockitoBean
    AuditService auditService;

    AuditFailureIntegrationTest(
            MockMvc mockMvc, EventRepository eventRepository, AuditRecordRepository auditRecordRepository) {
        super(mockMvc, eventRepository, auditRecordRepository);
    }

    @Test
    @DisplayName("EV-23 falla al guardar la auditoría de un QUALITY → 500 INTERNAL_ERROR sin detalle y 0 eventos (rollback)")
    void givenAuditFailure_whenPostingQuality_thenInternalErrorAndEventRolledBack() throws Exception {
        willThrow(new IllegalStateException(INTERNAL_MESSAGE)).given(auditService).audit(any());

        postEvent(eventJson("EVT-2001", "QUALITY", "Quality check"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intentá de nuevo más tarde."))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(content().string(not(containsString(INTERNAL_MESSAGE))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));

        assertThat(eventRepository.count()).isZero();
        assertThat(auditRecordRepository.count()).isZero();
    }
}
