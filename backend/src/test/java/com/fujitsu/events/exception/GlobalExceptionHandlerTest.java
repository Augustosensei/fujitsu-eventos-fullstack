package com.fujitsu.events.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fujitsu.events.config.CorrelationIdFilter;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String INTERNAL_MESSAGE = "jdbc:h2:mem:secret password=s3cr3t";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ServletWebRequest request =
            new ServletWebRequest(new MockHttpServletRequest("POST", "/api/events"));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("OB-03 excepción inesperada → 500 INTERNAL_ERROR sin stack ni mensaje interno; log ERROR con stack")
    void givenUnexpectedException_whenHandling_thenGenericProblemAndErrorLogWithStack(CapturedOutput output) {
        MDC.put(CorrelationIdFilter.MDC_KEY, "abc-123");

        var response = handler.handleUnexpected(new IllegalStateException(INTERNAL_MESSAGE), request);

        var problem = problemOf(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(problem.getDetail()).isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_DETAIL);
        assertThat(problem.getProperties())
                .containsEntry(GlobalExceptionHandler.CODE, ErrorCode.INTERNAL_ERROR)
                .containsEntry(GlobalExceptionHandler.CORRELATION_ID, "abc-123")
                .doesNotContainKeys("trace", "stackTrace", "exception", "message");
        assertThat(problem.toString()).doesNotContain(INTERNAL_MESSAGE);
        assertThat(output.getOut())
                .contains("ERROR")
                .contains("java.lang.IllegalStateException: " + INTERNAL_MESSAGE)
                .contains("\tat ");
    }

    @Test
    @DisplayName("OB-03 error 5xx del framework sin code → se responde como INTERNAL_ERROR genérico")
    void givenFrameworkServerError_whenHandling_thenGenericInternalError() throws Exception {
        var response = handler.handleException(new HttpMessageNotWritableException(INTERNAL_MESSAGE), request);

        var problem = problemOf(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(problem.getDetail()).isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_DETAIL);
        assertThat(problem.getProperties()).containsEntry(GlobalExceptionHandler.CODE, ErrorCode.INTERNAL_ERROR);
        assertThat(problem.toString()).doesNotContain(INTERNAL_MESSAGE);
    }

    @Test
    @DisplayName("DT9 excepción de dominio → status del ErrorCode, título y mensaje del dominio como detail")
    void givenDomainException_whenHandling_thenProblemFromErrorCode() {
        var response = handler.handleDomain(new EventNotFoundException("EVT-404"), request);

        var problem = problemOf(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(problem.getTitle()).isEqualTo("Evento no encontrado");
        assertThat(problem.getDetail()).isEqualTo("No existe un evento con eventId 'EVT-404'.");
        assertThat(problem.getProperties()).containsEntry(GlobalExceptionHandler.CODE, ErrorCode.EVENT_NOT_FOUND);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    @DisplayName("DT9 DT26 cada ErrorCode tiene status HTTP y título en español")
    void givenErrorCode_whenMapping_thenExpectedStatusAndTitle(ErrorCode code) {
        var expected = Map.of(
                ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST, HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_PAGINATION, HttpStatus.BAD_REQUEST,
                ErrorCode.DUPLICATE_EVENT, HttpStatus.CONFLICT,
                ErrorCode.EVENT_NOT_FOUND, HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                ErrorCode.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCode.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCode.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE,
                ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(GlobalExceptionHandler.statusOf(code)).isEqualTo(expected.get(code));
        assertThat(GlobalExceptionHandler.titleOf(code)).isNotBlank();
    }

    private static ProblemDetail problemOf(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        return (ProblemDetail) response.getBody();
    }
}
