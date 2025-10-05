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
import java.time.Duration;
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
            // Normalize URL and prefer HTTPS
            String normalizedUrl = url.replaceAll("/+$", "");
            
            // If no protocol specified, try HTTPS first
            if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
                normalizedUrl = "https://" + normalizedUrl;
            }
            
            this.repositoryUrl = normalizedUrl + "/";
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
    
    public CompletableFuture<HealthStatus> testConnectionAsync() {
        if (repositoryUrl == null) {
            return CompletableFuture.completedFuture(HealthStatus.ERROR);
        }
        
        return CompletableFuture.supplyAsync(() -> testConnectionHealth());
    }
    
    private CompletableFuture<Void> testConnectionWithUrl(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "api/v1/health"))
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
                    // Parse simple JSON response like {"status": "UP"}
                    String healthResponse = response.body();
                    log.info("Health check response: {}", 
                        healthResponse.contains("\"status\":\"UP\"") ? "healthy" : "service issues detected");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse health check response", e);
                }
            });
    }
    
    private String generateAuthHeader() {
        if (!config.isUseAuthentication() || config.getRepositoryPassword() == null) {
            return null;
        }
        // Send plain text password - server will verify against stored BCrypt hash
        // Note: In production, this should only be used over HTTPS
        return config.getRepositoryPassword();
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
    
    public HealthStatus testConnectionHealth() {
        try {
            HttpRequest request = createRequestBuilder("/api/v1/health")
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 401) {
                return HealthStatus.ERROR;
            }
            if (response.statusCode() != 200) {
                return HealthStatus.ERROR;
            }
            
            // Try to parse health response
            try {
                // Parse as simple JSON with "status" field
                if (response.body().contains("\"status\":\"UP\"")) {
                    return HealthStatus.HEALTHY;
                } else if (response.body().contains("\"status\":\"DEGRADED\"")) {
                    return HealthStatus.DEGRADED;
                } else {
                    return HealthStatus.DEGRADED;
                }
            } catch (Exception e) {
                return HealthStatus.DEGRADED;
            }
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return HealthStatus.ERROR;
        }
    }
    
    public long getRepositorySize() {
        try {
            HttpRequest request = createRequestBuilder("/api/v1/repository/size")
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return Long.parseLong(response.body().trim());
            }
            
            // Fallback: calculate from mod sets
            List<ModSet> modSets = getModSets().join();
            return modSets.stream()
                .flatMap(ms -> ms.getMods() != null ? ms.getMods().stream() : java.util.stream.Stream.empty())
                .mapToLong(mod -> mod.getSize())
                .sum();
                
        } catch (Exception e) {
            log.error("Failed to get repository size", e);
            return 0;
        }
    }
}
