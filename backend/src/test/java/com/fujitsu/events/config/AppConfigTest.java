package com.fujitsu.events.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    @DisplayName("DT4 el Clock de la aplicación usa la zona por defecto del sistema")
    void givenAppConfig_whenCreatingClock_thenUsesSystemDefaultZone() {
        assertThat(new AppConfig().clock().getZone()).isEqualTo(ZoneId.systemDefault());
    }
}
