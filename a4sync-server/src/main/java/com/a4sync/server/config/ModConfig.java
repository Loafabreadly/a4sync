package com.a4sync.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(ModProperties.class)
public class ModConfig {
    private final ModProperties modProperties;
    private final ObjectMapper objectMapper;

    public ModConfig(ModProperties modProperties, ObjectMapper objectMapper) {
        this.modProperties = modProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Path rootPath() {
        return Path.of(modProperties.getRootDirectory());
    }

    @Bean
    public ObjectMapper modSetObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}


