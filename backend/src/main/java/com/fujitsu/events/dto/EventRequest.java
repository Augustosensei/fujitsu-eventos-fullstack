package com.fujitsu.events.dto;

import com.fujitsu.events.dto.json.EventTypeDeserializer;
import com.fujitsu.events.dto.json.IsoLocalDateTimeDeserializer;
import com.fujitsu.events.dto.json.StrictStringDeserializer;
import com.fujitsu.events.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import tools.jackson.databind.annotation.JsonDeserialize;

public record EventRequest(
        @Schema(example = "EVT-1001", maxLength = ValidEventId.MAX_LENGTH, pattern = ValidEventId.PATTERN)
                @JsonDeserialize(using = StrictStringDeserializer.class)
                @ValidEventId
                String eventId,
        @Schema(example = "PRODUCTION")
                @JsonDeserialize(using = EventTypeDeserializer.class)
                @NotNull(message = "{event.eventType.required}")
                EventType eventType,
        @Schema(example = "Production line started", maxLength = EventRequest.DESCRIPTION_MAX_LENGTH)
                @JsonDeserialize(using = StrictStringDeserializer.class)
                @NotBlank(message = "{event.description.required}")
                @Size(max = EventRequest.DESCRIPTION_MAX_LENGTH, message = "{event.description.size}")
                String description,
        @Schema(
                        example = "2026-05-20T10:00:00",
                        type = "string",
                        description = "ISO-8601 sin zona; si se omite lo asigna el servidor")
                @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
                LocalDateTime timestamp) {

    public static final int DESCRIPTION_MAX_LENGTH = 500;

    public EventRequest {
        eventId = eventId == null ? null : eventId.strip();
    }
}
