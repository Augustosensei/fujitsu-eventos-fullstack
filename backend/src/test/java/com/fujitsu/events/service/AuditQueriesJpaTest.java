package com.fujitsu.events.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.model.AuditRecord;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(ServiceSliceConfig.class)
class AuditQueriesJpaTest {

    private static final Instant EARLIER = Instant.parse("2026-05-20T13:00:00Z");
    private static final Instant LATER = Instant.parse("2026-05-20T13:05:00Z");

    private final TestEntityManager entityManager;
    private final AuditService auditService;

    AuditQueriesJpaTest(TestEntityManager entityManager, AuditService auditService) {
        this.entityManager = entityManager;
        this.auditService = auditService;
    }

    @Test
    @DisplayName("AU-02 auditorías ordenadas por auditedAt descendente y desempate por id descendente")
    void givenAuditsWithTiedAuditedAt_whenListing_thenNewestFirstThenHighestId() {
        persistAudit("EVT-OLD", EARLIER);
        persistAudit("EVT-TIE-1", LATER);
        persistAudit("EVT-TIE-2", LATER);

        var page = auditService.findPage(new PageQuery(0, 10));

        assertThat(page.content())
                .extracting(AuditResponse::eventId)
                .containsExactly("EVT-TIE-2", "EVT-TIE-1", "EVT-OLD");
        assertThat(page.content()).allSatisfy(audit -> assertThat(audit.id()).isNotNull());
    }

    @Test
    @DisplayName("AU-03 una página de auditorías con su eventId se lee en a lo sumo 2 sentencias (página + count)")
    void givenSeveralAudits_whenListingPage_thenNoNPlusOneQueries() {
        persistAudit("EVT-1", EARLIER);
        persistAudit("EVT-2", EARLIER);
        persistAudit("EVT-3", LATER);
        var statistics = statistics();
        statistics.clear();

        var page = auditService.findPage(new PageQuery(0, 2));

        assertThat(page.content()).extracting(AuditResponse::eventId).containsExactly("EVT-3", "EVT-2");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    private void persistAudit(String eventId, Instant auditedAt) {
        var event = entityManager.persist(new Event(
                eventId, EventType.QUALITY, "Check", LocalDateTime.of(2026, 5, 20, 10, 0), auditedAt));
        entityManager.persistAndFlush(new AuditRecord(event, auditedAt));
        entityManager.clear();
    }

    private Statistics statistics() {
        return entityManager.getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }
}
