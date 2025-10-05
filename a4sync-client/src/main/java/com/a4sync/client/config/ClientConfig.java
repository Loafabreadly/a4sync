package com.a4sync.client.config;

import com.a4sync.client.model.Repository;
import lombok.Data;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.a4sync.common.model.GameOptions;

@Data
public class ClientConfig {
    // Legacy single repository support (for backward compatibility)
    private String serverUrl;
    private String repositoryPassword;
    private boolean useAuthentication;
    
    // New multi-repository support
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
        if (serverUrl != null && !serverUrl.isEmpty() && repositories.isEmpty()) {
            Repository legacyRepo = new Repository("Default Repository", serverUrl);
            legacyRepo.setPassword(repositoryPassword);
            legacyRepo.setUseAuthentication(useAuthentication);
            repositories.add(legacyRepo);
        }
    }
}
