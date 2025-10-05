package com.a4sync.client.service;

import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RepositoryService {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    @Getter
    private String repositoryUrl;
    
    public RepositoryService() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public void connect(String url) {
        this.repositoryUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
    
    public CompletableFuture<List<ModSet>> getModSets() {
        if (repositoryUrl == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to repository"));
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(repositoryUrl + "/api/v1/modsets"))
                .GET()
                .build();
                
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, ModSet.class));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse mod sets", e);
                    }
                });
    }
    
    public CompletableFuture<ModSet> getAutoConfig() {
        if (repositoryUrl == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to repository"));
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(repositoryUrl + "/api/v1/autoconfig"))
                .GET()
                .build();
                
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, ModSet.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse auto config", e);
                    }
                });
    }
}
