package com.a4sync.client.service;

import com.a4sync.client.config.RepositoryConfig;
import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RepositoryManager {
    
    private final Path configPath;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ObservableList<RepositoryConfig> repositories;
    private final Map<String, Map<String, ModSet>> modSetCache;

    RepositoryManager() {
        // Get config directory first to initialize final field
        Path appDir = Path.of(System.getProperty("user.home"), ".a4sync");
        this.configPath = appDir.resolve("repositories.json");
        
        this.objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // For Instant serialization
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.repositories = FXCollections.observableArrayList();
        this.modSetCache = new ConcurrentHashMap<>();
        
        // Load repositories on initialization
        loadRepositories();
    }

    private void loadRepositories() {
        try {
            if (Files.exists(configPath)) {
                CollectionType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, RepositoryConfig.class);
                List<RepositoryConfig> loaded = objectMapper.readValue(configPath.toFile(), type);
                repositories.setAll(loaded);
                log.info("Loaded {} repositories from config", loaded.size());
            }
        } catch (Exception e) {
            log.error("Failed to load repositories", e);
        }
    }

    public void saveRepositories() {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writeValue(configPath.toFile(), new ArrayList<>(repositories));
            log.info("Saved {} repositories to config", repositories.size());
        } catch (Exception e) {
            log.error("Failed to save repositories", e);
        }
    }

    public ObservableList<RepositoryConfig> getRepositories() {
        return FXCollections.unmodifiableObservableList(repositories);
    }

    public void addRepository(RepositoryConfig config) {
        repositories.add(config);
        saveRepositories();
    }

    public void removeRepository(RepositoryConfig config) {
        repositories.remove(config);
        modSetCache.remove(config.getUrl());
        saveRepositories();
    }

    public void updateRepository(RepositoryConfig config) {
        int index = repositories.indexOf(config);
        if (index >= 0) {
            repositories.set(index, config);
            saveRepositories();
        }
    }

    public CompletableFuture<List<ModSetStatus>> checkRepositories() {
        return CompletableFuture.supplyAsync(() -> {
            List<ModSetStatus> statuses = new ArrayList<>();
            
            for (RepositoryConfig repo : repositories) {
                if (!repo.isEnabled() || !repo.isCheckOnStartup()) {
                    continue;
                }

                try {
                    // Fetch remote mod sets
                    String url = repo.getUrl() + "/api/modsets";
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json");
                        
                    // Add authentication if enabled
                    if (repo.isEnabled() && repo.getUsername() != null && !repo.getUsername().isEmpty()) {
                        String auth = repo.getUsername() + ":" + repo.getPassword();
                        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                        requestBuilder.header("Authorization", "Basic " + encodedAuth);
                    }
                    
                    HttpRequest request = requestBuilder.build();
                        
                    HttpResponse<String> response = httpClient.send(
                        request, 
                        HttpResponse.BodyHandlers.ofString()
                    );
                    
                    if (response.statusCode() != 200) {
                        throw new IOException("Server returned " + response.statusCode());
                    }
                    
                    ModSet[] remoteSets = objectMapper.readValue(
                        response.body(), 
                        ModSet[].class
                    );
                    
                    if (remoteSets == null) {
                        log.warn("No mod sets found for repository: {}", repo.getName());
                        continue;
                    }

                    // Check each mod set
                    for (ModSet remoteSet : remoteSets) {
                        Path localPath = repo.getLocalPath()
                            .resolve("modsets")
                            .resolve(remoteSet.getName() + ".json");
                            
                        if (!Files.exists(localPath)) {
                            statuses.add(ModSetStatus.notDownloaded(repo, remoteSet));
                            continue;
                        }

                        ModSet localSet = objectMapper.readValue(localPath.toFile(), ModSet.class);
                        if (!localSet.getVersion().equals(remoteSet.getVersion())) {
                            statuses.add(ModSetStatus.updateAvailable(repo, remoteSet, localSet));
                        }
                    }
                    
                    repo.setLastChecked(Instant.now());
                    repo.setLastError(null);
                    
                } catch (Exception e) {
                    log.error("Failed to check repository: {}", repo.getName(), e);
                    repo.setLastError(e.getMessage());
                }
            }
            
            saveRepositories();
            return statuses;
        });
    }

    @Value
    public static class ModSetStatus {
        public enum Status {
            NOT_DOWNLOADED,
            UPDATE_AVAILABLE,
            UP_TO_DATE
        }

        RepositoryConfig repository;
        ModSet remoteSet;
        ModSet localSet;
        Status status;

        public static ModSetStatus notDownloaded(RepositoryConfig repo, ModSet remoteSet) {
            return new ModSetStatus(repo, remoteSet, null, Status.NOT_DOWNLOADED);
        }

        public static ModSetStatus updateAvailable(RepositoryConfig repo, ModSet remoteSet, ModSet localSet) {
            return new ModSetStatus(repo, remoteSet, localSet, Status.UPDATE_AVAILABLE);
        }

        public static ModSetStatus upToDate(RepositoryConfig repo, ModSet remoteSet, ModSet localSet) {
            return new ModSetStatus(repo, remoteSet, localSet, Status.UP_TO_DATE);
        }
    }
}