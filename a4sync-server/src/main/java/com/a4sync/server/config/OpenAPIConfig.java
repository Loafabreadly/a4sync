package com.a4sync.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    OpenAPI a4syncOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("A4Sync Server API")
                .description("API documentation for A4Sync mod synchronization server")
                .version("1.0.0")
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")));
    }
}