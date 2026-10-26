package com.fujitsu.events.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fujitsu.events.config.PaginationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageQueryValidationTest {

    private static final int MAX_SIZE = 100;

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        var configuration = Validation.byDefaultProvider().configure();
        var defaultFactory = configuration.getDefaultConstraintValidatorFactory();
        validatorFactory = configuration
                .constraintValidatorFactory(new PropertiesAwareFactory(defaultFactory))
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("LS-04 size igual al máximo configurado es válido")
    void givenSizeEqualToMax_whenValidating_thenNoViolations() {
        assertThat(validator.validate(new PageQuery(0, MAX_SIZE))).isEmpty();
    }

    @Test
    @DisplayName("LS-04 size máximo + 1 se rechaza con el máximo configurado en el mensaje")
    void givenSizeAboveMax_whenValidating_thenViolationMentionsMax() {
        var violations = validator.validate(new PageQuery(0, MAX_SIZE + 1));

        assertThat(messagesOf(violations))
                .containsExactly("size: El tamaño de página puede ser como máximo 100.");
    }

    @Test
    @DisplayName("LS-04 size 0 y page -1 se rechazan")
    void givenZeroSizeAndNegativePage_whenValidating_thenBothAreRejected() {
        var violations = validator.validate(new PageQuery(-1, 0));

        assertThat(messagesOf(violations))
                .containsExactlyInAnyOrder(
                        "page: El número de página debe ser 0 o mayor.",
                        "size: El tamaño de página debe ser 1 o mayor.");
    }

    @Test
    @DisplayName("LS-03 page y size ausentes son válidos (se aplican los defaults)")
    void givenMissingPageAndSize_whenValidating_thenNoViolations() {
        assertThat(validator.validate(new PageQuery(null, null))).isEmpty();
    }

    private static Set<String> messagesOf(Set<ConstraintViolation<PageQuery>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toSet());
    }

    private record PropertiesAwareFactory(ConstraintValidatorFactory delegate)
            implements ConstraintValidatorFactory {

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            if (key == MaxPageSizeValidator.class) {
                return key.cast(new MaxPageSizeValidator(new PaginationProperties(20, MAX_SIZE)));
            }
            return delegate.getInstance(key);
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
            delegate.releaseInstance(instance);
        }
    }
}
