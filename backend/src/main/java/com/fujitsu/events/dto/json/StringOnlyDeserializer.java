package com.fujitsu.events.dto.json;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public abstract sealed class StringOnlyDeserializer<T> extends StdDeserializer<T>
        permits StrictStringDeserializer, EventTypeDeserializer, IsoLocalDateTimeDeserializer {

    protected StringOnlyDeserializer(Class<T> valueType) {
        super(valueType);
    }

    @Override
    public final T deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            throw context.wrongTokenException(parser, handledType(), JsonToken.VALUE_STRING, "expected a JSON string");
        }
        return fromString(parser.getString(), context);
    }

    protected abstract T fromString(String value, DeserializationContext context);

    protected DatabindException invalidFormat(DeserializationContext context, String value, String reason) {
        return context.weirdStringException(value, handledType(), reason);
    }
}
