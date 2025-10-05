package com.a4sync.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ModProperties.class, DiscordProperties.class})
public class DiscordConfig {
}