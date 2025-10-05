package com.a4sync.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(ModProperties.class)
public class ModConfig {
    private final ModProperties modProperties;
    private final ObjectMapper objectMapper;

    public ModConfig(ModProperties modProperties) {
        this.modProperties = modProperties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Bean
    public Path rootPath() {
        return Path.of(modProperties.getRootDirectory());
    }

    @Bean
    public ObjectMapper modSetObjectMapper() {
        return objectMapper;
    }
}


