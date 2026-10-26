package com.fujitsu.events.dto.json;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import tools.jackson.databind.DeserializationContext;

public final class IsoLocalDateTimeDeserializer extends StringOnlyDeserializer<LocalDateTime> {

    public IsoLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    protected LocalDateTime fromString(String value, DeserializationContext context) {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            throw invalidFormat(context, value, "expected ISO-8601 local date-time without offset");
        }
    }
}
