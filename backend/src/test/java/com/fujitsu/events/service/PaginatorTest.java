package com.fujitsu.events.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.dto.PageQuery;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PaginatorTest {

    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "receivedAt", "id");

    private final Paginator paginator = new Paginator(new PaginationProperties(20, 100));

    @Test
    @DisplayName("LS-03 page y size ausentes usan page=0 y el size por defecto configurado")
    void givenMissingPageAndSize_whenConverting_thenDefaultsAreApplied() {
        assertThat(paginator.toPageable(new PageQuery(null, null), SORT)).isEqualTo(PageRequest.of(0, 20, SORT));
    }

    @Test
    @DisplayName("LS-03 page y size informados se respetan")
    void givenPageAndSize_whenConverting_thenTheyAreKept() {
        assertThat(paginator.toPageable(new PageQuery(3, 7), SORT)).isEqualTo(PageRequest.of(3, 7, SORT));
    }

    @Test
    @DisplayName("LS-06 una página dentro del rango consultable se delega a la consulta")
    void givenRegularPage_whenFetching_thenFinderReceivesPageable() {
        var received = new ArrayList<Pageable>();

        var page = paginator.fetch(new PageQuery(2, 10), SORT, pageable -> {
            received.add(pageable);
            return new PageImpl<>(List.of("x"), pageable, 21);
        }, () -> 21);

        assertThat(received).containsExactly(PageRequest.of(2, 10, SORT));
        assertThat(page.getContent()).containsExactly("x");
    }

    @Test
    @DisplayName("LS-06 una página cuyo offset supera el int de JPA devuelve vacío con el total real")
    void givenOffsetBeyondJpaRange_whenFetching_thenEmptyPageWithTotalWithoutQuerying() {
        var finderCalls = new ArrayList<Pageable>();

        Page<String> page = paginator.fetch(new PageQuery(Integer.MAX_VALUE, 20), SORT, pageable -> {
            finderCalls.add(pageable);
            return Page.empty(pageable);
        }, () -> 3);

        assertThat(finderCalls).isEmpty();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getNumber()).isEqualTo(Integer.MAX_VALUE);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
