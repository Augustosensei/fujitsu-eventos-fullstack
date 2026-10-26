package com.fujitsu.events.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicReference<String> mdcDuringChain = new AtomicReference<>();

    @ParameterizedTest(name = "X-Correlation-Id = {0}")
    @ValueSource(strings = {"abc-123", "A", "0123456789012345678901234567890123456789012345678901234567890123"})
    @DisplayName("OB-01 X-Correlation-Id válido → se reutiliza en MDC durante el request y en el header")
    void givenValidCorrelationId_whenFiltering_thenReusedInMdcAndResponse(String correlationId) throws Exception {
        filter.doFilter(requestWithHeader(correlationId), response, chainCapturingMdc());

        assertThat(mdcDuringChain).hasValue(correlationId);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(correlationId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @ParameterizedTest(name = "X-Correlation-Id = [{0}]")
    @NullSource
    @ValueSource(strings = {
        "",
        "con espacio",
        "under_score",
        "01234567890123456789012345678901234567890123456789012345678901234"
    })
    @DisplayName("OB-02 X-Correlation-Id ausente o inválido → UUID nuevo; MDC limpio tras el request")
    void givenMissingOrInvalidCorrelationId_whenFiltering_thenGeneratesUuid(String correlationId) throws Exception {
        filter.doFilter(requestWithHeader(correlationId), response, chainCapturingMdc());

        var generated = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(UUID.fromString(generated)).hasToString(generated);
        assertThat(generated).isNotEqualTo(correlationId);
        assertThat(mdcDuringChain).hasValue(generated);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("OB-02 la cadena lanza una excepción → el MDC igual queda limpio")
    void givenFailingChain_whenFiltering_thenMdcCleared() {
        var failingChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                    throws ServletException {
                throw new ServletException("boom");
            }
        };

        assertThatThrownBy(() -> filter.doFilter(requestWithHeader("abc-123"), response, failingChain))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private MockFilterChain chainCapturingMdc() {
        return new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                mdcDuringChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
        };
    }

    private static MockHttpServletRequest requestWithHeader(String correlationId) {
        var request = new MockHttpServletRequest("GET", "/api/events");
        if (correlationId != null) {
            request.addHeader(CorrelationIdFilter.HEADER, correlationId);
        }
        return request;
    }
}
