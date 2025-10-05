package com.a4sync.client.service;

import com.a4sync.common.model.A4SyncConfig;
import com.a4sync.client.model.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class A4SyncConfigService {
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public A4SyncConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Auto-discover and register modules
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Load A4Sync configuration from a local file
     */
    public A4SyncConfig loadFromFile(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }
        
        try {
            return objectMapper.readValue(configPath.toFile(), A4SyncConfig.class);
        } catch (Exception e) {
            log.error("Failed to parse A4Sync configuration file: {}", configPath, e);
            throw new IOException("Invalid A4Sync configuration file format", e);
        }
    }
    
    /**
     * Download and load A4Sync configuration from a URL
     */
    public CompletableFuture<A4SyncConfig> loadFromUrl(String configUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(configUrl))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " while fetching config from: " + configUrl);
                }
                
                return objectMapper.readValue(response.body(), A4SyncConfig.class);
                
            } catch (Exception e) {
                log.error("Failed to load A4Sync configuration from URL: {}", configUrl, e);
                throw new RuntimeException("Failed to load configuration from URL", e);
            }
        });
    }
    
    /**
     * Save A4Sync configuration to a file
     */
    public void saveToFile(A4SyncConfig config, Path outputPath) throws IOException {
        try {
            Files.createDirectories(outputPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(outputPath.toFile(), config);
            log.info("Saved A4Sync configuration to: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to save A4Sync configuration to: {}", outputPath, e);
            throw new IOException("Failed to save configuration file", e);
        }
    }
    
    /**
     * Convert A4SyncConfig to Repository model for internal use
     */
    public Repository configToRepository(A4SyncConfig config) {
        Repository repository = new Repository();
        
        // Generate unique ID from base URL
        String baseUrl = config.getConnection().getBaseUrl();
        repository.setId(generateRepositoryId(baseUrl));
        repository.setName(config.getRepository().getName());
        repository.setUrl(baseUrl);
        repository.setNotes(config.getRepository().getDescription());
        
        // Map authentication settings
        if (config.getConnection().isRequiresAuthentication()) {
            repository.setUseAuthentication(true);
            // Note: Password would be set separately by user input
        }
        
        return repository;
    }
    
    /**
     * Auto-discover A4Sync configuration from a repository URL
     * Tries common paths like /a4sync.json, /.a4sync, /config/a4sync.json
     */
    public CompletableFuture<A4SyncConfig> autoDiscoverConfig(String baseUrl) {
        String[] configPaths = {
                "/a4sync.json",
                "/.a4sync",
                "/config/a4sync.json",
                "/api/v1/config",
                "/repository.a4sync"
        };
        
        return CompletableFuture.supplyAsync(() -> {
            for (String configPath : configPaths) {
                try {
                    String configUrl = baseUrl.replaceAll("/$", "") + configPath;
                    A4SyncConfig config = loadFromUrl(configUrl).get();
                    log.info("Found A4Sync configuration at: {}", configUrl);
                    return config;
                } catch (Exception e) {
                    log.debug("Config not found at: {}{}", baseUrl, configPath);
                    // Continue trying other paths
                }
            }
            
            // If no config found, create a minimal default
            log.info("No A4Sync configuration found, creating default for: {}", baseUrl);
            return A4SyncConfig.createDefault("Unknown Repository", baseUrl);
        });
    }
    
    /**
     * Generate repository configuration and serve it at /a4sync.json
     * This method helps server administrators create the configuration
     */
    public A4SyncConfig generateServerConfig(
            String repositoryName,
            String baseUrl,
            String maintainer,
            boolean requiresAuth) {
        
        A4SyncConfig config = A4SyncConfig.createDefault(repositoryName, baseUrl);
        
        // Update with server-specific settings
        config.getRepository().setMaintainer(maintainer);
        config.getConnection().setRequiresAuthentication(requiresAuth);
        
        if (requiresAuth) {
            config.getConnection().setAuthenticationMethod("basic");
        }
        
        return config;
    }
    
    private String generateRepositoryId(String url) {
        // Simple hash-based ID generation from URL
        return "repo_" + Math.abs(url.hashCode());
    }
    
    /**
     * Validate A4SyncConfig structure and settings
     */
    public void validateConfig(A4SyncConfig config) throws IllegalArgumentException {
        if (config.getRepository() == null) {
            throw new IllegalArgumentException("Repository metadata is required");
        }
        
        if (config.getConnection() == null) {
            throw new IllegalArgumentException("Connection settings are required");
        }
        
        if (config.getConnection().getBaseUrl() == null || 
            config.getConnection().getBaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL is required");
        }
        
        try {
            URI.create(config.getConnection().getBaseUrl());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid base URL format", e);
        }
        
        log.info("A4Sync configuration validation passed");
    }
}