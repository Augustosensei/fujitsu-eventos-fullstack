package com.fujitsu.events.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-wide settings.
 *
 * @param version Project version shown in the OpenAPI document. Maven fills it in from the pom when packaging.
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(@NotBlank String version) {}
