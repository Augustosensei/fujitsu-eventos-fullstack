package com.fujitsu.events.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.pagination")
public record PaginationProperties(@Positive int defaultSize, @Positive int maxSize) {

    @AssertTrue(message = "app.pagination.default-size no puede superar app.pagination.max-size")
    public boolean isDefaultSizeWithinMax() {
        return defaultSize <= maxSize;
    }
}
