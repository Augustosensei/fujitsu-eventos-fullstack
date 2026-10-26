package com.fujitsu.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.exception.EventNotFoundException;
import com.fujitsu.events.model.Event;
import com.fujitsu.events.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ServiceSliceConfig.class)
class EventQueriesJpaTest {

    private static final Instant EARLIER = Instant.parse("2026-05-20T13:00:00Z");
    private static final Instant LATER = Instant.parse("2026-05-20T13:05:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventService eventService;

    @Test
    @DisplayName("LS-02 eventos ordenados por receivedAt descendente y desempate por id descendente")
    void givenEventsWithTiedReceivedAt_whenListing_thenNewestFirstThenHighestId() {
        persist("EVT-OLD", EARLIER);
        persist("EVT-TIE-1", LATER);
        persist("EVT-TIE-2", LATER);

        var page = eventService.findPage(new PageQuery(0, 10));

        assertThat(page.content())
                .extracting(EventResponse::eventId)
                .containsExactly("EVT-TIE-2", "EVT-TIE-1", "EVT-OLD");
    }

    @Test
    @DisplayName("LS-06 una página más allá del total devuelve content vacío con el total real")
    void givenThreeEvents_whenRequestingPageBeyondTotal_thenEmptyContent() {
        persistThree();

        var page = eventService.findPage(new PageQuery(5, 2));

        assertThat(page.content()).isEmpty();
        assertThat(page).extracting("page", "size", "totalElements", "totalPages").containsExactly(5, 2, 3L, 2);
    }

    @Test
    @DisplayName("LS-06 page=2147483647 devuelve content vacío sin desbordar el offset")
    void givenThreeEvents_whenRequestingMaxIntPage_thenEmptyContentWithoutOverflow() {
        persistThree();

        var page = eventService.findPage(new PageQuery(Integer.MAX_VALUE, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page).extracting("page", "totalElements").containsExactly(Integer.MAX_VALUE, 3L);
    }

    @Test
    @DisplayName("GT-04 un eventId que difiere solo en mayúsculas no encuentra el evento")
    void givenStoredUppercaseEventId_whenFindingLowercase_thenNotFound() {
        persist("EVT-1001", EARLIER);

        assertThat(eventService.findByEventId("EVT-1001").eventId()).isEqualTo("EVT-1001");
        assertThatThrownBy(() -> eventService.findByEventId("evt-1001"))
                .isInstanceOf(EventNotFoundException.class);
    }

    private void persistThree() {
        persist("EVT-A", EARLIER);
        persist("EVT-B", EARLIER);
        persist("EVT-C", LATER);
    }

    private void persist(String eventId, Instant receivedAt) {
        entityManager.persistAndFlush(new Event(
                eventId, EventType.PRODUCTION, "Description", LocalDateTime.of(2026, 5, 20, 10, 0), receivedAt));
        entityManager.clear();
    }
}
