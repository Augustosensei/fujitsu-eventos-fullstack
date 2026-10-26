package com.fujitsu.events.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventTypeTest {

    @ParameterizedTest(name = "{0} → requiresAudit = {1}")
    @CsvSource({"PRODUCTION, false", "QUALITY, true"})
    @DisplayName("DT2 solo QUALITY requiere auditoría")
    void givenEventType_whenRequiresAudit_thenOnlyQualityIsAudited(EventType type, boolean expected) {
        assertThat(type.requiresAudit()).isEqualTo(expected);
    }
}
