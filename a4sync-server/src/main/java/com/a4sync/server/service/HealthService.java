package com.a4sync.server.service;

import com.a4sync.server.config.ModProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final ModProperties modProperties;
    
    public HealthStatus checkHealth() {
        List<String> warnings = new ArrayList<>();
        
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
            warnings.add("Root directory is not writable: " + rootDir);
        }
        
        // Check disk space
        try {
            long freeSpace = Files.getFileStore(rootDir).getUsableSpace();
            long totalSpace = Files.getFileStore(rootDir).getTotalSpace();
            double freePercentage = (double) freeSpace / totalSpace * 100;
            
            if (freePercentage < 5) {
                throw new IllegalStateException("Critical: Less than 5% disk space remaining");
            } else if (freePercentage < 15) {
                warnings.add("Warning: Less than 15% disk space remaining");
            }
        } catch (IOException e) {
            warnings.add("Could not check disk space: " + e.getMessage());
        }
        
        // Return degraded status if we have warnings but no critical issues
        return warnings.isEmpty() ? HealthStatus.UP : HealthStatus.DEGRADED;
    }
    
    public enum HealthStatus {
        UP, DEGRADED, DOWN
    }
}