package com.fujitsu.events.dto.json;

import com.fujitsu.events.model.EventType;
import java.util.Arrays;
import tools.jackson.databind.DeserializationContext;

public final class EventTypeDeserializer extends StringOnlyDeserializer<EventType> {

    private static final String ALLOWED_VALUES = Arrays.toString(EventType.values());

    public EventTypeDeserializer() {
        super(EventType.class);
    }

    @Override
    protected EventType fromString(String value, DeserializationContext context) {
        return Arrays.stream(EventType.values())
                .filter(type -> type.name().equals(value))
                .findFirst()
                .orElseThrow(() -> invalidFormat(context, value, "allowed values: " + ALLOWED_VALUES));
    }
}
