package com.fujitsu.events.controller;

import com.fujitsu.events.dto.AuditResponse;
import com.fujitsu.events.dto.PageQuery;
import com.fujitsu.events.dto.PageResponse;
import com.fujitsu.events.service.AuditService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auditorías", description = "Auditorías generadas por los eventos QUALITY")
@RestController
@RequestMapping(path = "/api/audits", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "Listar auditorías", description = "Paginado, de la más reciente a la más antigua.")
    @ApiResponse(responseCode = "200", description = "Página de auditorías")
    @ApiResponse(
            responseCode = "400",
            description = "Parámetros de paginación inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "406",
            description = "Accept no admite JSON",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public PageResponse<AuditResponse> findPage(@Valid @ParameterObject PageQuery query) {
        return auditService.findPage(query);
    }
}
