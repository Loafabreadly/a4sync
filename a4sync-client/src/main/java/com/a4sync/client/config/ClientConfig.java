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
    
    public Path getSteamPath() {
        return steamPath;
    }
    
    public Path getGamePath() {
        return gamePath;
    }
    
    public GameOptions getDefaultGameOptionsObject() {
        return defaultGameOptions;
    }
    
    // Configuration persistence methods
    private static final String CONFIG_FILE_NAME = "a4sync-client-config.json";
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".a4sync");
    private static final Path CONFIG_FILE_PATH = CONFIG_DIR.resolve(CONFIG_FILE_NAME);
    
    public void saveConfig() {
        // Save to working directory if config exists there (portable mode)
        // Otherwise save to user home directory (standard mode)
        
        Path workingDirConfigPath = Paths.get(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        Path targetPath = CONFIG_FILE_PATH; // Default to user home
        
        if (Files.exists(workingDirConfigPath)) {
            // Config exists in working directory, save there (portable mode)
            targetPath = workingDirConfigPath;
        } else {
            // Save to user home directory (standard mode)
            try {
                Files.createDirectories(CONFIG_DIR);
            } catch (IOException e) {
                System.err.println("Failed to create config directory: " + e.getMessage());
                throw new RuntimeException("Could not create config directory", e);
            }
        }
        
        try {
            // Configure ObjectMapper for JSON serialization
            ObjectMapper mapper = createObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            // Write configuration to file
            mapper.writeValue(targetPath.toFile(), this);
            
            System.out.println("Configuration saved to: " + targetPath);
            
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
            throw new RuntimeException("Could not save client configuration", e);
        }
    }
    
    public static ClientConfig loadConfig() {
        // Priority order for loading configuration:
        // 1. Working directory (portable mode)
        // 2. User home directory (standard mode)
        // 3. Create new default config
        
        Path workingDirConfigPath = Paths.get(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        
        // First, try loading from working directory (portable mode)
        if (Files.exists(workingDirConfigPath)) {
            try {
                ObjectMapper mapper = createObjectMapper();
                ClientConfig config = mapper.readValue(workingDirConfigPath.toFile(), ClientConfig.class);
                System.out.println("Configuration loaded from working directory: " + workingDirConfigPath);
                return config;
            } catch (IOException e) {
                System.err.println("Failed to load configuration from working directory: " + e.getMessage());
                System.err.println("Falling back to user home directory configuration...");
            }
        }
        
        // Second, try loading from user home directory (standard mode)  
        if (Files.exists(CONFIG_FILE_PATH)) {
            try {
                ObjectMapper mapper = createObjectMapper();
                ClientConfig config = mapper.readValue(CONFIG_FILE_PATH.toFile(), ClientConfig.class);
                System.out.println("Configuration loaded from user directory: " + CONFIG_FILE_PATH);
                return config;
            } catch (IOException e) {
                System.err.println("Failed to load configuration from user directory: " + e.getMessage());
            }
        }
        
        // Third, create new default configuration
        System.out.println("No existing configuration found, creating new default config.");
        return new ClientConfig();
    }
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
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
