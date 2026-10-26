package com.fujitsu.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.mapper.AuditMapperImpl;
import com.fujitsu.events.model.AuditRecord;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.model.EventType;
import com.fujitsu.events.repository.AuditRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-20T13:00:00Z");

    @Mock
    private AuditRecordRepository auditRecordRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(
                auditRecordRepository,
                new AuditMapperImpl(),
                new Paginator(new PaginationProperties(20, 100)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("AU-02 la página de auditorías se pide por auditedAt e id descendentes y expone el eventId")
    void givenPageQuery_whenListingAudits_thenNewestFirstAndEventIdExposed() {
        var event = new Event("EVT-Q1", EventType.QUALITY, "Check", LocalDateTime.of(2026, 5, 20, 10, 0), NOW);
        var expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "auditedAt", "id"));
        when(auditRecordRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new AuditRecord(event, NOW)), expectedPageable, 6));

        var page = auditService.findPage(new PageQuery(1, 5));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(auditRecordRepository).findAll(pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(expectedPageable);
        assertThat(page.content()).containsExactly(new AuditResponse(null, "EVT-Q1", NOW));
        assertThat(page).extracting("page", "size", "totalElements", "totalPages").containsExactly(1, 5, 6L, 2);
    }
}
