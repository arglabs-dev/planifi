package com.planifi.backend.application.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BootstrapConfigLoaderTest {

    @Test
    void loadsYamlConfiguration() throws Exception {
        String payload = """
                version: v1
                storage:
                  provider: local
                  local:
                    basePath: /var/lib/planifi
                security:
                  rateLimit:
                    enabled: true
                    requestsPerMinute: 120
                    burst: 20
                users:
                  - email: admin@example.com
                    fullName: Admin
                    password: change-me
                    accounts:
                      - name: Principal
                        currency: MXN
                """;
        Path file = Files.createTempFile("planifi-bootstrap", ".yml");
        Files.writeString(file, payload);

        BootstrapConfigLoader loader = new BootstrapConfigLoader(
                new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator());

        BootstrapConfig config = loader.load(file);

        assertThat(config.users()).hasSize(1);
        assertThat(config.users().getFirst().accounts()).hasSize(1);
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        String payload = """
                {
                  "version": "v1",
                  "storage": {
                    "provider": "local",
                    "local": { "basePath": "/tmp" }
                  },
                  "security": {
                    "rateLimit": {
                      "enabled": true,
                      "requestsPerMinute": 60,
                      "burst": 10
                    }
                  },
                  "users": [
                    { "email": "user@example.com", "fullName": "User" }
                  ]
                }
                """;
        Path file = Files.createTempFile("planifi-bootstrap", ".json");
        Files.writeString(file, payload);

        BootstrapConfigLoader loader = new BootstrapConfigLoader(
                new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator());

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(BootstrapConfigException.class)
                .hasMessageContaining("password");
    }
}
