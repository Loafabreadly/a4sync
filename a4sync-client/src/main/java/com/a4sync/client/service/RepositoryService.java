package com.a4sync.client.service;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.exception.AuthenticationFailedException;
import com.a4sync.client.exception.RateLimitExceededException;
import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RepositoryService {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final ClientConfig config;
    private String repositoryUrl;
    
    public RepositoryService(ClientConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        setRepositoryUrl(config.getServerUrl());
    }
    
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public void setRepositoryUrl(String url) {
        if (url != null) {
            // Normalize URL to ensure it ends with a single /
            this.repositoryUrl = url.replaceAll("/+$", "") + "/";
        } else {
            this.repositoryUrl = null;
        }
    }
    
    private record HealthStatus(String status, String message) {}
    
    public CompletableFuture<Void> testConnection() {
        if (repositoryUrl == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Repository URL not set"));
        }
        
        HttpRequest request = createRequestBuilder("api/v1/health")
            .GET()
            .build();
            
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() == 401) {
                    throw new AuthenticationFailedException("Invalid repository password");
                }
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Repository unavailable: " + response.statusCode());
                }
                
                try {
                    var status = objectMapper.readValue(response.body(), HealthStatus.class);
                    if (!"UP".equals(status.status())) {
                        throw new RuntimeException("Repository is DOWN: " + status.message());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse health check response", e);
                }
            });
    }
    
    private String generateAuthHeader() {
        if (!config.isUseAuthentication() || config.getRepositoryPassword() == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(config.getRepositoryPassword().getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    private HttpRequest.Builder createRequestBuilder(String path) {
        if (repositoryUrl == null) {
            throw new IllegalStateException("Not connected to repository");
        }
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(repositoryUrl + path));
            
        String authHeader = generateAuthHeader();
        if (authHeader != null) {
            builder.header("X-Repository-Auth", authHeader);
        }
        
        return builder;
    }
    
    public CompletableFuture<List<ModSet>> getModSets() {
        HttpRequest request = createRequestBuilder("/api/v1/modsets")
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 429) {
                    String retryAfter = response.headers()
                        .firstValue("X-Rate-Limit-Retry-After")
                        .orElse("60");
                    log.warn("Rate limit hit, retry after {} seconds", retryAfter);
                    throw new RateLimitExceededException(Long.parseLong(retryAfter));
                }
                if (response.statusCode() == 401) {
                    throw new AuthenticationFailedException("Invalid repository password");
                }
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Unexpected response: " + response.statusCode());
                }
                return response.body();
            })
            .thenApply(body -> {
                try {
                    return List.of(objectMapper.readValue(body, ModSet[].class));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse mod sets", e);
                }
            });
    }
    
    public CompletableFuture<ModSet> getAutoConfig() {
        HttpRequest request = createRequestBuilder("/api/v1/autoconfig")
            .GET()
            .build();
            
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 429) {
                    String retryAfter = response.headers()
                        .firstValue("X-Rate-Limit-Retry-After")
                        .orElse("60");
                    log.warn("Rate limit hit, retry after {} seconds", retryAfter);
                    throw new RateLimitExceededException(Long.parseLong(retryAfter));
                }
                if (response.statusCode() == 401) {
                    throw new AuthenticationFailedException("Invalid repository password");
                }
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Unexpected response: " + response.statusCode());
                }
                try {
                    return objectMapper.readValue(response.body(), ModSet.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse auto config", e);
                }
            });
    }
    
    public CompletableFuture<byte[]> downloadChunk(String chunkId, long start, long length) {
        HttpRequest request = createRequestBuilder("/api/v1/chunks/" + chunkId)
            .header("Range", String.format("bytes=%d-%d", start, start + length - 1))
            .GET()
            .build();
            
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(response -> {
                if (response.statusCode() == 429) {
                    String retryAfter = response.headers()
                        .firstValue("X-Rate-Limit-Retry-After")
                        .orElse("60");
                    log.warn("Rate limit hit, retry after {} seconds", retryAfter);
                    throw new RateLimitExceededException(Long.parseLong(retryAfter));
                }
                if (response.statusCode() == 401) {
                    throw new AuthenticationFailedException("Invalid repository password");
                }
                if (response.statusCode() != 206 && response.statusCode() != 200) {
                    throw new RuntimeException("Unexpected response: " + response.statusCode());
                }
                byte[] data = response.body();
                if (data.length != length) {
                    throw new RuntimeException(
                        String.format("Expected %d bytes but got %d", length, data.length));
                }
                return data;
            });
    }
}
