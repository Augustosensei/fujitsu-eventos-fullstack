package com.fujitsu.events.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfigurationMetadataTest {

    private static final String METADATA = "META-INF/spring-configuration-metadata.json";

    @Test
    @DisplayName("OB-08 las propiedades propias (app.*) quedan documentadas para el IDE con tipo y descripción")
    void givenCompiledApp_whenReadingConfigurationMetadata_thenCustomPropertiesAreDescribed() throws IOException {
        Map<String, JsonNode> properties = customProperties();

        assertThat(properties).containsOnlyKeys("app.pagination.default-size", "app.pagination.max-size", "app.version");
        assertThat(properties.get("app.pagination.default-size").path("type").asString()).isEqualTo("java.lang.Integer");
        assertThat(properties.get("app.version").path("type").asString()).isEqualTo("java.lang.String");
        assertThat(properties.values())
                .allSatisfy(property -> assertThat(property.path("description").asString()).isNotBlank());
    }

    private static Map<String, JsonNode> customProperties() throws IOException {
        try (InputStream metadata = ConfigurationMetadataTest.class.getClassLoader().getResourceAsStream(METADATA)) {
            assertThat(metadata).as(METADATA).isNotNull();
            JsonNode properties = JsonMapper.builder().build().readTree(metadata).path("properties");
            return StreamSupport.stream(properties.spliterator(), false)
                    .filter(property -> property.path("name").asString().startsWith("app."))
                    .collect(Collectors.toMap(property -> property.path("name").asString(), property -> property));
        }
    }
}
