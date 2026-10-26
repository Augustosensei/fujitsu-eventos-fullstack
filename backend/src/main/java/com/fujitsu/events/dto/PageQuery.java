package com.fujitsu.events.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PageQuery(
        @Schema(description = "Número de página (desde 0)", defaultValue = "0")
                @PositiveOrZero(message = "{pagination.page.min}")
                Integer page,
        @Schema(description = "Tamaño de página (por defecto y máximo configurables: 20 y 100)")
                @Positive(message = "{pagination.size.min}")
                @MaxPageSize
                Integer size) {}
