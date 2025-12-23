package com.planifi.backend.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI planifiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Planifi Backend API")
                        .description("Scaffold for MCP-first expense capture")
                        .version("v0.1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Architecture baseline")
                        .url("./docs/ARCHITECTURE.md"));
    }
}
