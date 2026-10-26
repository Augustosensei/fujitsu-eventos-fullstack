package com.fujitsu.events.dto.json;

import tools.jackson.databind.DeserializationContext;

public final class StrictStringDeserializer extends StringOnlyDeserializer<String> {

    public StrictStringDeserializer() {
        super(String.class);
    }

    @Override
    protected String fromString(String value, DeserializationContext context) {
        return value;
    }
}
