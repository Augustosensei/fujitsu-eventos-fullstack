package com.fujitsu.events.service;

import com.fujitsu.events.config.PaginationProperties;
import com.fujitsu.events.mapper.AuditMapperImpl;
import com.fujitsu.events.mapper.EventMapperImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({EventService.class, AuditService.class, Paginator.class, EventMapperImpl.class, AuditMapperImpl.class})
class ServiceSliceConfig {

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2026-05-20T13:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    PaginationProperties paginationProperties() {
        return new PaginationProperties(20, 100);
    }
}
