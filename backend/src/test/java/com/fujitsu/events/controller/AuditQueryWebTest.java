package com.fujitsu.events.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;

class AuditQueryWebTest extends WebLayerTestSupport {

    AuditQueryWebTest(MockMvc mockMvc) {
        super(mockMvc);
    }

    @Test
    @DisplayName("AU-02 sin page ni size → el servicio aplica los defaults y la página se serializa completa")
    void givenNoPaginationParams_whenListingAudits_thenPageWithAuditFields() throws Exception {
        var audit = new AuditResponse(7L, "EVT-1001", Instant.parse("2026-05-20T13:00:00.789Z"));
        given(auditService.findPage(any())).willReturn(new PageResponse<>(List.of(audit), 0, 20, 1, 1));

        mockMvc.perform(get("/api/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].eventId").value("EVT-1001"))
                .andExpect(jsonPath("$.content[0].auditedAt").value("2026-05-20T13:00:00.789Z"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditService).findPage(new PageQuery(null, null));
    }

    @Test
    @DisplayName("AU-02 page y size válidos → llegan al servicio")
    void givenValidPaginationParams_whenListingAudits_thenForwarded() throws Exception {
        given(auditService.findPage(any())).willReturn(new PageResponse<>(List.of(), 3, 5, 0, 0));

        mockMvc.perform(get("/api/audits").param("page", "3").param("size", "5")).andExpect(status().isOk());

        verify(auditService).findPage(new PageQuery(3, 5));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({"size, 101", "size, 0", "page, -1", "page, abc", "page, 2147483648"})
    @DisplayName("AU-02 auditorías con las mismas reglas de paginación (LS-04/LS-05) → 400 INVALID_PAGINATION")
    void givenInvalidPagination_whenListingAudits_thenInvalidPagination(String param, String value) throws Exception {
        mockMvc.perform(get("/api/audits").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.errors[0].field").value(param));
        verifyNoInteractions(auditService);
    }
}
