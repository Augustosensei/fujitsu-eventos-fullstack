package com.fujitsu.events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI eventsOpenApi(AppProperties appProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("API de eventos de fábrica")
                        .version(appProperties.version())
                        .description("""
                                Registro de eventos PRODUCTION y QUALITY enviados por equipos externos, \
                                sin duplicados por eventId; los QUALITY generan un registro de auditoría. \
                                Los errores se devuelven como ProblemDetail (RFC 9457) con las propiedades \
                                code, errors y correlationId."""));
    }
}
