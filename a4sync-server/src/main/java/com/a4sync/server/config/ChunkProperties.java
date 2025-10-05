package com.a4sync.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "a4sync.chunks")
public class ChunkProperties {
    private int maxSize = 50 * 1024 * 1024;  // Default 50MB chunks
    private int parallel = 3;                 // Default parallel downloads
    private boolean validateHashes = true;    // Validate chunk hashes
    private long minFreeSpace = 10L * 1024L * 1024L * 1024L; // 10GB min free space
}