package com.fujitsu.events.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginationPropertiesTest {

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

    @Test
    @DisplayName("DT8 los valores por defecto (20 y 100) son válidos")
    void givenDefaultValues_whenValidating_thenNoViolations() {
        assertThat(validator.validate(new PaginationProperties(20, 100))).isEmpty();
    }

    @Test
    @DisplayName("DT8 tamaños no positivos se rechazan al arrancar")
    void givenNonPositiveSizes_whenValidating_thenViolationsOnBothProperties() {
        var violations = validator.validate(new PaginationProperties(0, -1));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("defaultSize", "maxSize");
    }

    @Test
    @DisplayName("DT8 un default mayor que el máximo se rechaza al arrancar")
    void givenDefaultAboveMax_whenValidating_thenConsistencyViolation() {
        var violations = validator.validate(new PaginationProperties(101, 100));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("app.pagination.default-size no puede superar app.pagination.max-size");
    }
}
