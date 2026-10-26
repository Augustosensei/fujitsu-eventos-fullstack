package com.fujitsu.events.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.fujitsu.events.repository.AuditRecordRepository;
import com.fujitsu.events.repository.EventRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

class ConcurrentRegistrationIntegrationTest extends IntegrationTestSupport {

    private static final int THREADS = 10;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final URI eventsUri;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    ConcurrentRegistrationIntegrationTest(
            MockMvc mockMvc,
            EventRepository eventRepository,
            AuditRecordRepository auditRecordRepository,
            @LocalServerPort int port) {
        super(mockMvc, eventRepository, auditRecordRepository);
        this.eventsUri = URI.create("http://localhost:" + port + "/api/events");
    }

    @Test
    @DisplayName("EV-20 10 POST concurrentes con el mismo eventId QUALITY → 1×201, 9×409; 1 evento y 1 auditoría")
    void givenConcurrentSameQualityEvent_whenPosting_thenExactlyOneRegisteredAndAudited() throws Exception {
        var request = HttpRequest.newBuilder(eventsUri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventJson("EVT-RACE", "QUALITY", "Concurrent quality check")))
                .build();
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Integer>>();

        var executor = Executors.newFixedThreadPool(THREADS);
        try {
            Callable<Integer> send = () -> {
                start.await();
                return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            };
            for (var i = 0; i < THREADS; i++) {
                futures.add(executor.submit(send));
            }
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var future : futures) {
                statuses.add(future.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
            }

            assertThat(statuses.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())))
                    .containsOnly(entry(201, 1L), entry(409, 9L));
        } finally {
            executor.shutdownNow();
        }
        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(auditRecordRepository.count()).isEqualTo(1);
        assertThat(eventRepository.findByEventId("EVT-RACE")).isPresent();
    }
}
