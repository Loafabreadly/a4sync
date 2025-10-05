package com.a4sync.server.service;

import com.a4sync.server.config.ModProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final ModProperties modProperties;
    
    public void checkHealth() {
        // Check if root directory exists and is accessible
        Path rootDir = Path.of(modProperties.getRootDirectory());
        if (!Files.exists(rootDir)) {
            throw new IllegalStateException("Root directory does not exist: " + rootDir);
        }
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalStateException("Root path is not a directory: " + rootDir);
        }
        if (!Files.isReadable(rootDir)) {
            throw new IllegalStateException("Root directory is not readable: " + rootDir);
        }
        if (!Files.isWritable(rootDir)) {
            throw new IllegalStateException("Root directory is not writable: " + rootDir);
        }
    }
}