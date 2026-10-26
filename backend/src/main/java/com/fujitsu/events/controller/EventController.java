package com.fujitsu.events.controller;

import com.fujitsu.events.dto.EventRequest;
import com.fujitsu.events.dto.EventResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import com.fujitsu.events.dto.ValidEventId;
import com.fujitsu.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Eventos", description = "Registro y consulta de eventos de fábrica")
@RestController
@RequestMapping(path = EventController.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EventController {

    static final String BASE_PATH = "/api/events";

    private final EventService eventService;

    @InitBinder("eventId")
    void stripPathEventId(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StripStringEditor());
    }

    @Operation(
            summary = "Registrar un evento",
            description = "PRODUCTION solo se registra; QUALITY se registra y genera una auditoría. "
                    + "Un eventId ya registrado no se vuelve a guardar ni a auditar.")
    @ApiResponse(responseCode = "201", description = "Evento registrado; Location apunta al evento")
    @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o cuerpo mal formado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "406",
            description = "Accept no admite JSON",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "El eventId ya estaba registrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "415",
            description = "Content-Type distinto de application/json",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponse> register(@Valid @RequestBody EventRequest request) {
        var registered = eventService.register(request);
        var location = UriComponentsBuilder.fromPath(BASE_PATH + "/{eventId}")
                .encode()
                .buildAndExpand(registered.eventId())
                .toUri();
        return ResponseEntity.created(location).body(registered);
    }

    @Operation(summary = "Listar eventos", description = "Paginado, del más reciente al más antiguo según la recepción.")
    @ApiResponse(responseCode = "200", description = "Página de eventos")
    @ApiResponse(
            responseCode = "400",
            description = "Parámetros de paginación inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "406",
            description = "Accept no admite JSON",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PageResponse<EventResponse> findPage(@Valid @ParameterObject PageQuery query) {
        return eventService.findPage(query);
    }

    @Operation(summary = "Obtener un evento por eventId", description = "El eventId se recorta antes de validarlo.")
    @ApiResponse(responseCode = "200", description = "Evento encontrado")
    @ApiResponse(
            responseCode = "400",
            description = "eventId inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No existe un evento con ese eventId",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "406",
            description = "Accept no admite JSON",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{eventId}")
    public EventResponse findByEventId(@PathVariable @ValidEventId String eventId) {
        return eventService.findByEventId(eventId);
    }
}
