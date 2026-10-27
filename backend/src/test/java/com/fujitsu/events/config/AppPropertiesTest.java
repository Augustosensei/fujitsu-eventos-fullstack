package com.fujitsu.events.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.1-SNAPSHOT", "1.0.0"})
    @DisplayName("DT40 una versión informada es válida")
    void givenVersion_whenValidating_thenNoViolations(String version) {
        assertThat(validator.validate(new AppProperties(version))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("DT40 sin app.version la aplicación no arranca")
    void givenMissingOrBlankVersion_whenValidating_thenViolationOnVersion(String version) {
        assertThat(validator.validate(new AppProperties(version)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("version");
    }
}
