package com.fujitsu.events.service;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.dto.PageQuery;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Paginator {

    private static final int FIRST_PAGE = 0;
    private static final long JPA_MAX_FIRST_RESULT = Integer.MAX_VALUE;

    private final PaginationProperties paginationProperties;

    public <T> Page<T> fetch(
            PageQuery query, Sort sort, Function<Pageable, Page<T>> finder, LongSupplier totalCounter) {
        var pageable = toPageable(query, sort);
        if (pageable.getOffset() > JPA_MAX_FIRST_RESULT) {
            return new PageImpl<>(List.of(), pageable, totalCounter.getAsLong());
        }
        return finder.apply(pageable);
    }

    Pageable toPageable(PageQuery query, Sort sort) {
        var page = Objects.requireNonNullElse(query.page(), FIRST_PAGE);
        var size = Objects.requireNonNullElse(query.size(), paginationProperties.defaultSize());
        return PageRequest.of(page, size, sort);
    }
}
