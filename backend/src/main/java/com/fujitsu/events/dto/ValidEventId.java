package com.fujitsu.events.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@NotBlank(message = "{event.eventId.required}")
@Size(max = ValidEventId.MAX_LENGTH, message = "{event.eventId.size}")
@Pattern(regexp = ValidEventId.PATTERN, message = "{event.eventId.pattern}")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventId {

    int MAX_LENGTH = 64;
    String PATTERN = "^[A-Za-z0-9_-]*$";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
