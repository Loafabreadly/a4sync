package com.a4sync.client.config;

import com.a4sync.client.model.Repository;
import com.a4sync.common.model.GameType;
import lombok.Data;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.a4sync.common.model.GameOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;

@Data
public class ClientConfig {
    // Legacy single repository support (for backward compatibility)
    private String serverUrl;
    private String repositoryPassword;
    private boolean useAuthentication;
    
    // New multi-repository support
    @JsonProperty("repositories")
    private List<Repository> repositories = new ArrayList<>();
    
    private String localModsPath;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private List<Path> modDirectories = new ArrayList<>();
    private Path steamPath;
    private Path gamePath;
    private GameOptions defaultGameOptions = new GameOptions();

    public void addModDirectory(Path path) {
        if (!modDirectories.contains(path)) {
            modDirectories.add(path);
        }
    }
    
    public void removeModDirectory(Path path) {
        modDirectories.remove(path);
    }

    public List<Path> getModDirectories() {
        return Collections.unmodifiableList(modDirectories);
    }
    
    // Repository management methods
    public void addRepository(Repository repository) {
        if (repositories.stream().noneMatch(r -> r.getId().equals(repository.getId()))) {
            repositories.add(repository);
        }
    }
    
    public void removeRepository(String repositoryId) {
        repositories.removeIf(r -> r.getId().equals(repositoryId));
    }
    
    public void updateRepository(Repository repository) {
        for (int i = 0; i < repositories.size(); i++) {
            if (repositories.get(i).getId().equals(repository.getId())) {
                repositories.set(i, repository);
                return;
            }
        }
        // If not found, add it
        repositories.add(repository);
    }
    
    public List<Repository> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }
    
    public List<Repository> getEnabledRepositories() {
        return repositories.stream()
            .filter(Repository::isEnabled)
            .toList();
    }
    
    public Repository getRepositoryById(String id) {
        return repositories.stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    // Migration method for backward compatibility
    public void migrateLegacyRepository() {
        // Simplified - do nothing for now
    }
    
    // Add getters that return proper types (Lombok @Data should handle these, but adding for clarity)
    public String getSteamPathAsString() {
        return steamPath != null ? steamPath.toString() : null;
    }
    
    public String getGamePathAsString() {
        return gamePath != null ? gamePath.toString() : null;
    }
    
    public GameOptions getDefaultGameOptionsObject() {
        return defaultGameOptions;
    }
    
    // Configuration persistence methods
    private static final String CONFIG_FILE_NAME = "a4sync-client-config.json";
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".a4sync");
    private static final Path CONFIG_FILE_PATH = CONFIG_DIR.resolve(CONFIG_FILE_NAME);
    
    public void saveConfig() {
        try {
            // Ensure config directory exists
            Files.createDirectories(CONFIG_DIR);
            
            // Configure ObjectMapper for JSON serialization
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            // Write configuration to file
            mapper.writeValue(CONFIG_FILE_PATH.toFile(), this);
            
            System.out.println("Configuration saved to: " + CONFIG_FILE_PATH);
            
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
            throw new RuntimeException("Could not save client configuration", e);
        }
    }
    
    public static ClientConfig loadConfig() {
        if (!Files.exists(CONFIG_FILE_PATH)) {
            System.out.println("No existing configuration found, creating new config.");
            return new ClientConfig();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            ClientConfig config = mapper.readValue(CONFIG_FILE_PATH.toFile(), ClientConfig.class);
            System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
            return config;
            
        } catch (IOException e) {
            System.err.println("Failed to load configuration, using defaults: " + e.getMessage());
            return new ClientConfig();
        }
    }
    
    // Add missing methods for MultiRepositoryService compatibility
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public void saveDefaultGameOptions(GameOptions options) {
        this.defaultGameOptions = options;
        saveConfig(); // Persist the configuration
    }
    
    public void saveSteamPath(Path path) {
        this.steamPath = path;
        saveConfig();
    }
    
    public void saveGamePath(Path path) {
        this.gamePath = path;
        saveConfig();
    }
}
