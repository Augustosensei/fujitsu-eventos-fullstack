package com.fujitsu.events.exception;

import com.fujitsu.events.config.CorrelationIdFilter;
import com.fujitsu.events.dto.FieldErrorResponse;
import com.fujitsu.events.dto.PageQuery;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String ERRORS = "errors";
    static final String CORRELATION_ID = "correlationId";

    static final String VALIDATION_DETAIL = "La solicitud tiene datos inválidos. Revisá los campos indicados.";
    static final String PAGINATION_DETAIL = "Los parámetros de paginación no son válidos.";
    static final String MALFORMED_DETAIL =
            "El cuerpo de la solicitud falta o no es un JSON válido con un objeto como raíz.";
    static final String RESOURCE_NOT_FOUND_DETAIL = "No existe el recurso solicitado.";
    static final String METHOD_NOT_ALLOWED_DETAIL = "El método %s no está permitido para este recurso.";
    static final String UNSUPPORTED_MEDIA_TYPE_DETAIL = "El tipo de contenido no es compatible; usá application/json.";
    static final String NOT_ACCEPTABLE_DETAIL = "La API solo puede responder en formato JSON.";
    static final String INTERNAL_ERROR_DETAIL = "Ocurrió un error inesperado. Intentá de nuevo más tarde.";

    private static final Comparator<FieldErrorResponse> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing(FieldErrorResponse::field).thenComparing(FieldErrorResponse::message);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Object> handleDomain(DomainException exception, WebRequest request) {
        return respond(exception, exception.getErrorCode(), exception.getMessage(), List.of(), new HttpHeaders(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        return respond(exception, ErrorCode.INTERNAL_ERROR, INTERNAL_ERROR_DETAIL, List.of(), new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var pagination = PageQuery.class.equals(exception.getParameter().getParameterType());
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return pagination
                ? respond(exception, ErrorCode.INVALID_PAGINATION, PAGINATION_DETAIL, errors, headers, request)
                : respond(exception, ErrorCode.VALIDATION_ERROR, VALIDATION_DETAIL, errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return respond(exception, ErrorCode.VALIDATION_ERROR, VALIDATION_DETAIL, errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return invalidBodyField(exception.getCause())
                .map(error -> respond(
                        exception, ErrorCode.VALIDATION_ERROR, VALIDATION_DETAIL, List.of(error), headers, request))
                .orElseGet(() -> respond(
                        exception, ErrorCode.MALFORMED_REQUEST, MALFORMED_DETAIL, List.of(), headers, request));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(
                exception, ErrorCode.RESOURCE_NOT_FOUND, RESOURCE_NOT_FOUND_DETAIL, List.of(), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var detail = METHOD_NOT_ALLOWED_DETAIL.formatted(exception.getMethod());
        return respond(exception, ErrorCode.METHOD_NOT_ALLOWED, detail, List.of(), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(
                exception,
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                UNSUPPORTED_MEDIA_TYPE_DETAIL,
                List.of(),
                headers,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return respond(exception, ErrorCode.NOT_ACCEPTABLE, NOT_ACCEPTABLE_DETAIL, List.of(), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (statusCode.is5xxServerError() && !hasCode(problem)) {
                return handleUnexpected(exception, request);
            }
            problem.setProperty(CORRELATION_ID, MDC.get(CorrelationIdFilter.MDC_KEY));
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR, MALFORMED_REQUEST, INVALID_PAGINATION -> HttpStatus.BAD_REQUEST;
            case DUPLICATE_EVENT -> HttpStatus.CONFLICT;
            case EVENT_NOT_FOUND, RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case NOT_ACCEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    static String titleOf(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR -> "Datos inválidos";
            case MALFORMED_REQUEST -> "Solicitud mal formada";
            case INVALID_PAGINATION -> "Paginación inválida";
            case DUPLICATE_EVENT -> "Evento duplicado";
            case EVENT_NOT_FOUND -> "Evento no encontrado";
            case RESOURCE_NOT_FOUND -> "Recurso no encontrado";
            case METHOD_NOT_ALLOWED -> "Método no permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
            case NOT_ACCEPTABLE -> "Formato de respuesta no disponible";
            case INTERNAL_ERROR -> "Error interno";
        };
    }

    private ResponseEntity<Object> respond(
            Exception exception,
            ErrorCode code,
            String detail,
            List<FieldErrorResponse> errors,
            HttpHeaders headers,
            WebRequest request) {
        var status = statusOf(code);
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(titleOf(code));
        problem.setProperty(CODE, code);
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS, errors.stream().sorted(BY_FIELD_THEN_MESSAGE).toList());
        }
        if (status.is5xxServerError()) {
            log.error("Request failed: {} -> {}", request.getDescription(false), code, exception);
        } else {
            log.warn("Request rejected: {} -> {} {}", request.getDescription(false), status.value(), code);
        }
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(CODE);
    }

    private static FieldErrorResponse toFieldError(FieldError error) {
        var message = error.isBindingFailure()
                ? "El parámetro '%s' debe ser un número entero válido.".formatted(error.getField())
                : error.getDefaultMessage();
        return new FieldErrorResponse(error.getField(), message);
    }

    private static Optional<FieldErrorResponse> invalidBodyField(Throwable cause) {
        if (!(cause instanceof MismatchedInputException mismatch) || mismatch.getPath().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mismatch.getPath().get(0).getPropertyName())
                .map(field -> new FieldErrorResponse(field, invalidValueMessage(field, mismatch.getTargetType())));
    }

    private static String invalidValueMessage(String field, Class<?> targetType) {
        if (targetType != null && targetType.isEnum()) {
            var allowed = Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "El valor de '%s' no es válido. Valores permitidos: %s.".formatted(field, allowed);
        }
        if (LocalDateTime.class.equals(targetType)) {
            return "'%s' debe tener formato ISO-8601 sin zona horaria, por ejemplo 2026-05-20T10:00:00."
                    .formatted(field);
        }
        return "'%s' debe ser un texto (string JSON).".formatted(field);
    }
}
