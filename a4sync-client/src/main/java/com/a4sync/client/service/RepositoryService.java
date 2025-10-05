package com.a4sync.client.service;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.exception.AuthenticationFailedException;
import com.a4sync.client.exception.RateLimitExceededException;
import com.a4sync.client.model.HealthStatus;
import com.a4sync.common.model.ModSet;
import com.a4sync.common.version.VersionInfo;
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
    
    /**
     * Checks version compatibility with the repository server.
     * @return A warning message if versions don't match, null otherwise
     */
    public CompletableFuture<String> checkVersionCompatibility() {
        if (repositoryUrl == null) {
            return CompletableFuture.completedFuture("Repository URL not set");
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(repositoryUrl + "api/version"))
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    try {
                        VersionInfo serverVersion = objectMapper.readValue(response.body(), VersionInfo.class);
                        return VersionInfo.getInstance().checkCompatibility(serverVersion);
                    } catch (Exception e) {
                        log.warn("Failed to parse server version info", e);
                        return "Failed to parse server version information";
                    }
                } else {
                    log.warn("Failed to get server version info, status: {}", response.statusCode());
                    return "Failed to get server version information";
                }
            })
            .exceptionally(throwable -> {
                log.warn("Failed to connect to server for version check", throwable);
                return "Failed to connect to server for version check";
            });
    }
    
    public CompletableFuture<Void> testConnection() {
        return switch (repositoryUrl) {
            case null -> CompletableFuture.failedFuture(
                new IllegalStateException("Repository URL not set"));
            default -> {
                HttpRequest request = createRequestBuilder("api/v1/health")
                    .GET()
                    .build();
                    
                yield client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 401) {
                            throw new AuthenticationFailedException("Invalid repository password");
                        }
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Repository unavailable: " + response.statusCode());
                        }
                        
                        try {
                            record HealthStatus(String status, String message) {}
                            var status = objectMapper.readValue(response.body(), HealthStatus.class);
                            if (!status.status().equals("UP")) {
                                throw new RuntimeException("Repository is DOWN: " + status.message());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse health check response", e);
                        }
                    });
            }
        };
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
