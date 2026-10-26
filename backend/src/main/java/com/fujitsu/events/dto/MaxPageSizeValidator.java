package com.fujitsu.events.dto;

import com.fujitsu.events.config.PaginationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

@RequiredArgsConstructor
public class MaxPageSizeValidator implements ConstraintValidator<MaxPageSize, Integer> {

    private final PaginationProperties paginationProperties;

    @Override
    public boolean isValid(Integer size, ConstraintValidatorContext context) {
        var maxSize = paginationProperties.maxSize();
        if (size == null || size <= maxSize) {
            return true;
        }
        context.unwrap(HibernateConstraintValidatorContext.class).addMessageParameter("max", maxSize);
        return false;
    }
}
