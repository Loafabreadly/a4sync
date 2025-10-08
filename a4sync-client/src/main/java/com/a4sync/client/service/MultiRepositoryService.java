package com.a4sync.client.service;

import com.a4sync.client.config.ClientConfig;
import com.a4sync.client.model.Repository;
import com.a4sync.client.model.HealthStatus;
import com.a4sync.common.model.ModSet;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class MultiRepositoryService {
    private final ClientConfig config;
    private final Map<String, RepositoryService> repositoryServices;
    
    public MultiRepositoryService(ClientConfig config) {
        this.config = config;
        this.repositoryServices = config.getRepositories().stream()
            .collect(Collectors.toMap(
                Repository::getId,
                repo -> createRepositoryService(repo)
            ));
    }
    
    private RepositoryService createRepositoryService(Repository repository) {
        // Create a temporary config for this repository
        ClientConfig repoConfig = new ClientConfig();
        repoConfig.setServerUrl(repository.getUrl());
        repoConfig.setRepositoryPassword(repository.getPassword());
        repoConfig.setUseAuthentication(repository.isUseAuthentication());
        repoConfig.setMaxRetries(config.getMaxRetries());
        repoConfig.setRetryDelayMs(config.getRetryDelayMs());
        
        return new RepositoryService(repoConfig);
    }
    
    public void addRepository(Repository repository) {
        config.addRepository(repository);
        repositoryServices.put(repository.getId(), createRepositoryService(repository));
    }
    
    /**
     * Add repository following the proper connection flow:
     * 1. Hit health API endpoint to determine repository state
     * 2. Hit modsets endpoint to get repository details and modset count
     * 3. Populate repository object with server information
     * @param repository The repository to add and validate
     * @return CompletableFuture that completes when the full flow is done
     */
    public CompletableFuture<Repository> addRepositoryWithValidation(Repository repository) {
        // Create temporary service for validation
        RepositoryService tempService = createRepositoryService(repository);
        
        // Step 1: Health API check
        return tempService.testConnectionAsync().thenCompose(healthStatus -> {
            repository.setHealthStatus(healthStatus);
            repository.setLastChecked(java.time.LocalDateTime.now());
            
            if (healthStatus == HealthStatus.ERROR) {
                throw new RuntimeException("Repository health check failed");
            }
            
            // Step 2: Get repository info and modset count
            return tempService.getRepositoryInfo();
        }).thenApply(repositoryInfo -> {
            // Step 3: Populate repository with server information
            repository.setName(repositoryInfo.getName());
            int modSetCount = repositoryInfo.getModSets() != null ? repositoryInfo.getModSets().size() : 0;
            repository.setModSetCount(modSetCount);
            repository.setLastUpdated(repositoryInfo.getLastUpdated());
            
            // Add to configuration and services
            addRepository(repository);
            
            return repository;
        });
    }
    
    public void removeRepository(String repositoryId) {
        config.removeRepository(repositoryId);
        repositoryServices.remove(repositoryId);
    }
    
    public CompletableFuture<Map<Repository, List<ModSet>>> getAllModSets() {
        List<CompletableFuture<Map.Entry<Repository, List<ModSet>>>> futures = 
            config.getEnabledRepositories().stream()
                .map(repo -> {
                    RepositoryService service = repositoryServices.get(repo.getId());
                    if (service != null) {
                        return service.getModSets()
                            .thenApply(modSets -> Map.entry(repo, modSets))
                            .exceptionally(throwable -> {
                                log.warn("Failed to get mod sets from repository {}: {}", 
                                    repo.getName(), throwable.getMessage());
                                return Map.entry(repo, List.<ModSet>of());
                            });
                    } else {
                        return CompletableFuture.completedFuture(Map.entry(repo, List.<ModSet>of()));
                    }
                })
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                )));
    }
    
    public CompletableFuture<String> checkVersionCompatibility(Repository repository) {
        RepositoryService service = repositoryServices.get(repository.getId());
        if (service != null) {
            return service.checkVersionCompatibility();
        }
        return CompletableFuture.completedFuture("Repository service not found");
    }
    
    public CompletableFuture<HealthStatus> testConnection(Repository repository) {
        RepositoryService service = repositoryServices.get(repository.getId());
        if (service != null) {
            return CompletableFuture.completedFuture(service.testConnectionHealth());
        }
        return CompletableFuture.completedFuture(HealthStatus.ERROR);
    }
    
    public RepositoryService getRepositoryService(String repositoryId) {
        return repositoryServices.get(repositoryId);
    }
    
    public List<Repository> getRepositories() {
        return config.getRepositories();
    }
    
    public List<Repository> getEnabledRepositories() {
        return config.getEnabledRepositories();
    }
}