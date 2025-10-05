package com.a4sync.client.config;

// import com.a4sync.client.model.Repository;
import lombok.Data;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.a4sync.common.model.GameOptions;

@Data
public class ClientConfig {
    // Legacy single repository support (for backward compatibility)
    private String serverUrl;
    private String repositoryPassword;
    private boolean useAuthentication;
    
    // New multi-repository support
    // @JsonProperty("repositories")
    // private List<Repository> repositories = new ArrayList<>();
    
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
    
    // Repository management methods (simplified for now)
    public void addRepository(Repository repository) {
        // TODO: Implement proper multi-repository support
    }
    
    public void removeRepository(String repositoryId) {
        // TODO: Implement proper multi-repository support
    }
    
    public void updateRepository(Repository repository) {
        // Simplified for now - update the legacy single repository settings
        if (repository.getUrl() != null) {
            this.serverUrl = repository.getUrl();
        }
        saveConfig();
    }
    
    public void removeModDirectory(Path path) {
        modDirectories.remove(path);
    }
    
    public List<Repository> getRepositories() {
        return new ArrayList<>(); // Return empty list for now
    }
    
    public List<Repository> getEnabledRepositories() {
        return new ArrayList<>(); // Return empty list for now
    }
    
    public Repository getRepositoryById(String id) {
        return null; // Return null for now
    }
    
    // Migration method for backward compatibility
    public void migrateLegacyRepository() {
        // Simplified - do nothing for now
    }
    
    // Add missing getters that are being called
    public String getServerUrl() {
        return serverUrl;
    }
    
    public String getSteamPath() {
        return steamPath;
    }
    
    public String getGamePath() {
        return gamePath;
    }
    
    public String getDefaultGameOptions() {
        return defaultGameOptions;
    }
    
    public String getRepositoryPassword() {
        return repositoryPassword;
    }
    
    public boolean isUseAuthentication() {
        return useAuthentication;
    }
    
    public int getMaxRetries() {
        return 3; // Default value
    }
    
    public int getRetryDelayMs() {
        return 1000; // Default value
    }
}
